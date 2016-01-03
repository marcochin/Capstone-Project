package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.data.ListEventQueue;
import com.mcochin.stockstreaks.utils.Utility;
import com.mcochin.stockstreaks.widget.StockWidgetProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

/**
 * Service that goes online to retrieve our information for our list item!
 */
public class MainService extends IntentService {
    private static final String TAG = MainService.class.getSimpleName();
    public static final String KEY_LOAD_SYMBOL_QUERY ="searchQuery";
    public static final String KEY_LOAD_A_FEW_QUERY ="loadAFewQuery";

    public static final String ACTION_LOAD_A_FEW = "actionLoadAFew";
    public static final String ACTION_LOAD_SYMBOL = "actionStockWithSymbol";
    public static final String ACTION_LOAD_WIDGET_REFRESH ="widgetRefreshQuery";

    //needs to be 32 and 366 since we need to compare closing to prev day's closing price
    private static final int MONTH = 32;

    private static final String NOT_AVAILABLE = "N/A";
    private static final String USD = "USD";
    private static final String NASDAQ = "NMS";
    private static final String NYSE = "NYQ";

    public MainService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        try {
            // check for internet
            if(!Utility.isNetworkAvailable(this)){
                throw new IOException(getString(R.string.toast_no_network));
            }

            switch(action) {
                case ACTION_LOAD_SYMBOL:
                    String symbol = intent.getStringExtra(KEY_LOAD_SYMBOL_QUERY);
                    performActionLoadSymbol(symbol);
                    break;

                case ACTION_LOAD_A_FEW:
                    String[] symbols = intent.getStringArrayExtra(KEY_LOAD_A_FEW_QUERY);
                    performActionLoadAFew(symbols);
                    break;

                case ACTION_LOAD_WIDGET_REFRESH:
                    performActionWidgetRefresh();
            }
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));

                if(e.getMessage().equals(getString(R.string.toast_no_network))
                        || e instanceof IllegalArgumentException){
                    Utility.showToast(this, e.getMessage());
                }else {
                    Utility.showToast(this, getString(R.string.toast_error_retrieving_data));
                }

            switch(action) {
                case ACTION_LOAD_SYMBOL:
                    ListEventQueue.getInstance().post(new LoadSymbolFinishedEvent(null, false));
                    break;

                case ACTION_LOAD_A_FEW:
                    ListEventQueue.getInstance().post(new LoadAFewFinishedEvent(null, false));
                    break;

                case ACTION_LOAD_WIDGET_REFRESH:
                    sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATE_ERROR));
                    break;
            }
        }
    }

    private void performActionLoadSymbol(String symbol)throws IOException, IllegalArgumentException{
        // Check if symbol already exists in database
        if (Utility.isEntryExist(symbol, getContentResolver())) {
            throw new IllegalArgumentException(getString(R.string.toast_placeholder_symbol_exists, symbol));
        }

        Stock stock = YahooFinance.get(symbol);
        ContentValues values = getLatestMainValues(stock);

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // Add insert operation to list
        ops.add(ContentProviderOperation
                .newInsert(StockEntry.buildUri(stock.getSymbol()))
                .withValues(values)
                .withYieldAllowed(true)
                .build());

        // Add update time operation to list
        ops.add(getUpdateTimeOperation());
        // Apply operations
        applyOperations(ops, StockProvider.METHOD_INSERT_ITEM, null);
    }

    private void performActionLoadAFew(String[] symbolsToLoad) throws IOException, IllegalArgumentException{
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        Map<String, Stock> stockList =  YahooFinance.get(symbolsToLoad);
        if(stockList != null) {
            for (String symbol : symbolsToLoad) {
                Stock stock = stockList.get(symbol);
                ContentValues values = getLatestMainValues(stock);

                // Add update operations to list
                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                        .withValues(values)
                        .withYieldAllowed(true)
                        .build());

            }

            // Save the shown list position every time load a few is performed. This is primarily
            // for the widget refreshes, but also for one edge case in which the list updates after
            // onPause() is called and the shown list position will not be reflected on next app
            // open if user exits our app.
            ops.add(getListPositionBookmarkOperation(symbolsToLoad[symbolsToLoad.length-1]));
            // Add update time operation to list
            ops.add(getUpdateTimeOperation());
            // Apply operations
            applyOperations(ops, StockProvider.METHOD_UPDATE_ITEMS, null);
        }
    }

    private void performActionWidgetRefresh()throws IOException, IllegalArgumentException{
        Cursor cursor = null;
        try{
            final String [] projection = new String[]{StockEntry.COLUMN_SYMBOL};
            final int indexSymbol = 0;

            // Update widget to reflect that we are currently updating
            sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATING));

            // Query db for the FIRST FEW as a normal refresh would do.
            cursor = getContentResolver().query(
                    StockEntry.CONTENT_URI,
                    projection,
                    StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION,
                    new String[]{Integer.toString(ListManipulator.A_FEW)},
                    StockProvider.ORDER_BY_LIST_POSITION_ASC_ID_DESC);

            if(cursor != null) {
                int cursorCount = cursor.getCount();
                String[] symbolsToLoad = new String[cursorCount];
                for(int i = 0; i < cursorCount; i++){
                    cursor.moveToPosition(i);
                    symbolsToLoad[i] = cursor.getString(indexSymbol);
                }

                if(symbolsToLoad.length != 0) {
                    performActionLoadAFew(symbolsToLoad);

                } else{
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                    // Add update time operation to list
                    ops.add(getUpdateTimeOperation());
                    // Apply operations
                    applyOperations(ops, StockProvider.METHOD_UPDATE_ITEMS, null);
                }
                // Update widget to reflect changes
                sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATED));
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    private ContentValues getLatestMainValues(Stock stock) throws IOException, IllegalArgumentException{
        float recentClose = 0;
        ContentValues values;

        if(stock == null){
            throw new IllegalArgumentException(getString(R.string.toast_error_retrieving_data));

        } else if (stock.getName().equals(NOT_AVAILABLE) || (!stock.getCurrency().equals(USD))) {
            throw new IllegalArgumentException(getString(R.string.toast_symbol_not_found));

        } else{
            // Get history from a month ago to today!
            Calendar nowTime = Utility.getNewYorkCalendarInstance();
            Calendar fromTime = Utility.getNewYorkCalendarInstance();
            fromTime.add(Calendar.DAY_OF_MONTH, -MONTH);

            // Download history from Yahoo
            List<HistoricalQuote> historyList =
                    stock.getHistory(fromTime, nowTime, Interval.DAILY);

            StockQuote quote = stock.getQuote();
            Calendar lastTradeTime = Utility.calendarTimeReset(quote.getLastTradeTime());
            Calendar firstHistoricalDate = Utility.calendarTimeReset(historyList.get(0).getDate());

            int nowTimeDay = nowTime.get(Calendar.DAY_OF_MONTH);
            int lastTradeDay = lastTradeTime.get(Calendar.DAY_OF_MONTH);

            // Determine if we should use stock price
            // "nowTimeDay > lastTradeDay" will cover holidays and weekends in which history has
            // not updated yet!
            if(!lastTradeTime.equals(firstHistoricalDate)
                    && (nowTimeDay > lastTradeDay || !Utility.isDuringTradingHours())){

                recentClose = Utility.roundTo2FloatDecimals(
                        stock.getQuote().getPrice().floatValue());
            }

            values = getValuesFromHistoryList(historyList, recentClose);
            values.put(StockEntry.COLUMN_SYMBOL, stock.getSymbol());
            values.put(StockEntry.COLUMN_FULL_NAME, stock.getName());
        }

        return values;
    }

    private ContentValues getValuesFromHistoryList(List<HistoricalQuote> historyList,
                                                   float recentClose){
        int streak = 0;
        long prevStreakEndDate = 0;
        float prevStreakEndPrice = 0;

        // Due to inconsistency of Yahoo History Dates sometimes being offset by 1
        // We can't determine the first up streak by looking at the change. We need to compare
        // to its previous adj close price. If it compares to itself, streak will not change.
        HistoricalQuote firstHistory = historyList.get(0);
        if(recentClose != 0){
            float firstHistoryAdjClose = Utility.roundTo2FloatDecimals(
                    firstHistory.getAdjClose().floatValue());

            if(recentClose > firstHistoryAdjClose){
                streak++;
            }else if(recentClose < firstHistoryAdjClose) {
                streak--;
            }

        }else{
            // Retrieves most recent close if not already retrieved.
            recentClose = Utility.roundTo2FloatDecimals(firstHistory.getAdjClose().floatValue());
        }

        for (int i = 0; i < historyList.size(); i++) {
            boolean shouldBreak = false;
            HistoricalQuote history = historyList.get(i);

            float historyAdjClose = Utility.roundTo2FloatDecimals(history.getAdjClose().floatValue());

            // Need to compare history adj close to its previous history's adj close.
            // http://budgeting.thenest.com/adjusted-closing-price-vs-closing-price-32457.html
            // If its the last day in the history we need to skip it because we have
            // nothing to compare it to.
            if (i + 1 < historyList.size()) {
                float prevHistoryAdjClose = Utility.roundTo2FloatDecimals(
                                historyList.get(i + 1).getAdjClose().floatValue());

                if (historyAdjClose > prevHistoryAdjClose) {
                    // Down streak broken so break;
                    if (streak < 0) {
                        shouldBreak = true;
                    } else {
                        streak++;
                    }
                } else if (historyAdjClose < prevHistoryAdjClose) {
                    // Up streak broken so break;
                    if (streak > 0) {
                        shouldBreak = true;
                    } else {
                        streak--;
                    }
                }
            }
            if (shouldBreak) {
                prevStreakEndDate = history.getDate().getTimeInMillis();
                prevStreakEndPrice = historyAdjClose;
                break;
            }
        }

        // Get change Dollar and change Percentage
        Pair changeDollarAndPercentage = Utility.calculateChange(recentClose, prevStreakEndPrice);

        ContentValues values = new ContentValues();
        values.put(StockEntry.COLUMN_RECENT_CLOSE, recentClose);
        values.put(StockEntry.COLUMN_STREAK, streak);
        values.put(StockEntry.COLUMN_CHANGE_DOLLAR, (float)changeDollarAndPercentage.first);
        values.put(StockEntry.COLUMN_CHANGE_PERCENT, (float)changeDollarAndPercentage.second);
        values.put(StockEntry.COLUMN_PREV_STREAK_END_PRICE, prevStreakEndPrice);
        values.put(StockEntry.COLUMN_PREV_STREAK_END_DATE, prevStreakEndDate);
        values.put(StockEntry.COLUMN_PREV_STREAK, 0);
        values.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, 0);
        values.put(StockEntry.COLUMN_STREAK_YEAR_LOW, 0);

        return values;
    }

    private ContentProviderOperation getUpdateTimeOperation(){
        ContentValues values = new ContentValues();
        values.put(SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI, System.currentTimeMillis());

        return ContentProviderOperation
                .newUpdate(SaveStateEntry.CONTENT_URI)
                .withValues(values)
                .withYieldAllowed(true)
                .build();
    }

    private ContentProviderOperation getListPositionBookmarkOperation(String symbol){
        Cursor cursor = null;
        int listPosition = ListManipulator.A_FEW; //Default to a few in case something goes wrong
        try{
            final String [] projection = new String[]{StockEntry.COLUMN_LIST_POSITION};
            final int indexListPosition = 0;

            cursor = getContentResolver().query(StockEntry.buildUri(symbol),
                    projection, null, null, null);

            if(cursor != null && cursor.moveToFirst()){
                listPosition = cursor.getInt(indexListPosition);
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put(SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK, listPosition);

        return ContentProviderOperation
                .newUpdate(SaveStateEntry.CONTENT_URI)
                .withValues(values)
                .withYieldAllowed(true)
                .build();
    }

    private void applyOperations(ArrayList<ContentProviderOperation> ops, String method, String arg){
        Bundle extras = new Bundle();
        extras.putParcelableArrayList(StockProvider.KEY_OPERATIONS, ops);

        getContentResolver().call(StockContract.BASE_CONTENT_URI, method, arg, extras);
    }
}