package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.utils.Utility;

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
 * Service that goes online to retrieve our information!
 */
public class NetworkService extends IntentService {
    private static final String TAG = NetworkService.class.getSimpleName();
    public static final String KEY_SEARCH_QUERY ="searchQuery";
    public static final String KEY_LOAD_A_FEW_QUERY ="loadAFewQuery";
    public static final String KEY_LIST_REFRESH ="updateUpdateDate";

    public static final String ACTION_LOAD_A_FEW = "actionLoadAFew";
    public static final String ACTION_STOCK_WITH_SYMBOL = "actionStockWithSymbol";
    public static final String ACTION_DETAILS = "actionDetails";

    //needs to be 32 and 366 since we need to compare closing to prev day's closing price
    private static final int MONTH = 32;
    private static final int YEAR = 366;

    private static final String NOT_AVAILABLE = "N/A";
    private static final String NASDAQ = "NMS";
    private static final String NYSE = "NYQ";

    public NetworkService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // check for internet
            if(!Utility.isNetworkAvailable(this)){
                Utility.showToast(this, getString(R.string.toast_no_network));
                return;
            }
            String action = intent.getAction();

            switch(action) {
                case ACTION_LOAD_A_FEW: {
                    String[] symbols = intent.getStringArrayExtra(KEY_LOAD_A_FEW_QUERY);
                    boolean listRefresh = intent.getBooleanExtra(KEY_LIST_REFRESH, false);

                    if(listRefresh || !Utility.canUpdateList(getContentResolver())){
                        performActionLoadAFew(symbols);
                    } else {
                        performActionLoadAFewNonLatest(symbols);
                    }
                    break;
                }

                case ACTION_STOCK_WITH_SYMBOL:
                    String symbol = intent.getStringExtra(KEY_SEARCH_QUERY);
                    performActionStockWithSymbol(symbol);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Utility.showToast(this, getString(R.string.toast_error_retrieving_data));
        }
    }

    private void performActionLoadAFew(String[] symbolsToLoad) throws IOException{
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        Map<String, Stock> stockList =  YahooFinance.get(symbolsToLoad);
        if(stockList != null) {
            for (Stock stock : stockList.values()) {
                ContentValues values = getLatestMainValues(stock);

                // Add update operations to list
                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                        .withValues(values)
                        .withYieldAllowed(true)
                        .build());

            }
            try {
                // Update the update time (Must be before applying the updates)
                updateUpdateTime();
                // Apply operations
                getContentResolver().applyBatch(StockContract.CONTENT_AUTHORITY, ops);

            }catch (RemoteException | OperationApplicationException e){
                Log.e(TAG, Log.getStackTraceString(e));
                Utility.showToast(this, getString(R.string.toast_error_updating_list));
            }
        }
    }

    private void performActionLoadAFewNonLatest(String[] symbolsToLoad) throws IOException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // Change symbols array to a stock array
        Stock[] stocks = new Stock[symbolsToLoad.length];
        for(int i = 0; i < symbolsToLoad.length; i++){
            Stock stock = new Stock(symbolsToLoad[i]);
            stocks[i] = stock;
        }

        // Create "To" Time
        Calendar lastUpdateTime = Utility.getLastUpdateTime(getContentResolver());
        if(Utility.isBeforeFourThirty(lastUpdateTime)){
            lastUpdateTime.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Create "From" Time
        Calendar fromTime = Utility.getNewYorkCalendarInstance();
        fromTime.setTimeInMillis(lastUpdateTime.getTimeInMillis());
        fromTime.add(Calendar.DAY_OF_MONTH, -YEAR);

        //Loop through stocks to get their history to calculate main values
        for(Stock stock: stocks){
            List<HistoricalQuote> historyList = stock.getHistory(fromTime, lastUpdateTime, Interval.DAILY);

            // Create ContentValues and put in ops
            ContentValues values = getValuesFromHistoryList(historyList, 0);

            // Add update operations to list
            ops.add(ContentProviderOperation
                    .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                    .withValues(values)
                    .withYieldAllowed(true)
                    .build());
        }
        try {
            // Apply operations
            getContentResolver().applyBatch(StockContract.CONTENT_AUTHORITY, ops);

        }catch (RemoteException | OperationApplicationException e){
            Log.e(TAG, Log.getStackTraceString(e));
            Utility.showToast(this, getString(R.string.toast_error_updating_list));
        }
    }

    private void performActionStockWithSymbol(String symbol)throws IOException{
        Stock stock = YahooFinance.get(symbol);
        ContentValues values = getLatestMainValues(stock);

        if(values == null){
            return;
        }
        // Update the update time (Must be before the insert)
        updateUpdateTime();
        // Put stock into the database
        getContentResolver().insert(StockEntry.buildUri(symbol), values);
    }

    private ContentValues getLatestMainValues(Stock stock) throws IOException{
        float recentClose = 0;
        ContentValues values = null;

        if(stock == null){
            Utility.showToast(this, getString(R.string.toast_error_retrieving_data));

        } else if (stock.getName().equals(NOT_AVAILABLE) || (!stock.getStockExchange().equals(NASDAQ)
                && !stock.getStockExchange().equals(NYSE))) {
            Utility.showToast(this, getString(R.string.toast_symbol_not_found));

        } else{
            // Get history from a month ago to today!
            Calendar nowTime = Utility.getNewYorkCalendarInstance();
            Calendar fromTime = Utility.getNewYorkCalendarInstance();
            fromTime.add(Calendar.DAY_OF_MONTH, -YEAR);

            // Download history from Yahoo
            List<HistoricalQuote> historyList =
                    stock.getHistory(fromTime, nowTime, Interval.DAILY);

            StockQuote quote = stock.getQuote();
            Calendar lastTradeTime = Utility.calendarTimeReset(quote.getLastTradeTime());
            Calendar firstHistoricalDate = Utility.calendarTimeReset(historyList.get(0).getDate());

            // "nowTimeDay > lastTradeDay" will cover holidays and weekends in which history has
            // not updated yet!
            int nowTimeDay = nowTime.get(Calendar.DAY_OF_MONTH);
            int lastTradeDay = lastTradeTime.get(Calendar.DAY_OF_MONTH);

            // Determine if we should use stock price
            if(!lastTradeTime.equals(firstHistoricalDate)
                    && (nowTimeDay > lastTradeDay || !Utility.isDuringTradingHours())){
                Log.d(TAG, "using stock price");
                recentClose = stock.getQuote().getPrice().floatValue();
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
        int prevStreak = 0;
        int yearStreakHigh = 0;
        int yearStreakLow = 0;
        int historyStreak = 0;

        // Due to inconsistency of Yahoo History Dates sometimes being offset by 1
        // We can't determine the first up streak by looking at the change. We need to compare
        // to its previous adj close price. So if it compares to itself, streak will not change.
        HistoricalQuote firstHistory = historyList.get(0);
        if(recentClose != 0){
            float firstHistoryAdjClose =
                    Utility.roundTo2Decimals(firstHistory.getAdjClose().floatValue());

            if(recentClose > firstHistoryAdjClose){
                historyStreak++;
            }else if(recentClose < firstHistoryAdjClose) {
                historyStreak--;
            }

        }else{
            // Retrieves most recent close if not already retrieved.
            recentClose = firstHistory.getAdjClose().floatValue();
        }

        for (int i = 0; i < historyList.size(); i++) {
            HistoricalQuote history = historyList.get(i);
            float historyAdjClose = Utility.roundTo2Decimals(history.getAdjClose().floatValue());
            boolean shouldBreak = false;

            // Need to compare history adj close to its previous history's adj close.
            // http://budgeting.thenest.com/adjusted-closing-price-vs-closing-price-32457.html
            // If its the last day in the history we need to skip it because we have
            // nothing to compare it to.
            if (i + 1 < historyList.size()) {
                float prevHistoryAdjClose =
                        Utility.roundTo2Decimals(historyList.get(i + 1).getAdjClose().floatValue());

                if (historyAdjClose > prevHistoryAdjClose) {
                    // Down streak broken so break;
                    if (historyStreak < 0) {
                        shouldBreak = true;
                    } else {
                        historyStreak++;
                    }
                } else if (historyAdjClose < prevHistoryAdjClose) {
                    // Up streak broken so break;
                    if (historyStreak > 0) {
                        shouldBreak = true;
                    } else {
                        historyStreak--;
                    }
                }
            }
            if (shouldBreak) {
                if(streak == 0) {
                    streak = historyStreak;
                    prevStreakEndDate = history.getDate().getTimeInMillis();
                    prevStreakEndPrice = historyAdjClose;
                } else if (prevStreak == 0){
                    prevStreak = historyStreak;
                }

                //set the history high and lows if historyStreak exceeds them
                if(historyStreak > yearStreakHigh){
                    yearStreakHigh = historyStreak;
                } else if (historyStreak < yearStreakLow){
                    yearStreakLow = historyStreak;
                }

                //reset historyStreak to whatever broke the streak so we don't skip it
                if(historyStreak > 0){
                    historyStreak = -1;
                }else{
                    historyStreak = 1;
                }
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
        values.put(StockEntry.COLUMN_PREV_STREAK, prevStreak);
        values.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, yearStreakHigh);
        values.put(StockEntry.COLUMN_STREAK_YEAR_LOW, yearStreakLow);

        Log.d(TAG, prevStreak + " " + yearStreakHigh + " " + yearStreakLow);

        return values;
    }

    private void updateUpdateTime(){
        ContentValues values = new ContentValues();
        values.put(SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI, System.currentTimeMillis());

        int rowsAffected = getContentResolver().update(SaveStateEntry.CONTENT_URI,
                values, null, null);

        if(rowsAffected < 1){
            getContentResolver().insert(SaveStateEntry.CONTENT_URI, values);
        }
    }

//    private ContentValues getDetailValues(String symbol) throws IOException{
//        Cursor cursor = null;
//        int historyStreak = 0;
//        int prevStreak = 0;
//        int yearStreakHigh = 0;
//        int yearStreakLow = 0;
//
//        ContentValues values;
//
//        try {
//            //projection
//            final String[] projection = new String[]{
//                    StockEntry.COLUMN_PREV_STREAK_END_DATE,
//                    StockEntry.COLUMN_STREAK
//            };
//
//            //indexes for the projection
//            final int indexPrevStreakEndDate = 0;
//            final int indexStreak = 1;
//
//
//            // Get streak end date and streak from the specified symbol
//            cursor = getContentResolver().query(
//                    StockContract.StockEntry.buildUri(symbol),
//                    projection,
//                    null,
//                    null,
//                    null);
//
//            long prevStreakEndDate = 0;
//
//            if (cursor != null && cursor.moveToFirst()) {
//                prevStreakEndDate = cursor.getLong(indexPrevStreakEndDate);
//                int streak = cursor.getInt(indexStreak);
//
//                //set its current streak to yearHigh or yearLow
//                if(streak > 0 ){
//                    yearStreakHigh = streak;
//                } else if (streak < 0){
//                    yearStreakLow = streak;
//                }
//            }
//
//            Calendar fromCalendar = Utility.getNewYorkCalendarInstance();
//            fromCalendar.add(Calendar.DAY_OF_MONTH, -YEAR); //We want 1 year of history
//            Calendar toCalendar = Utility.getNewYorkCalendarInstance();
//            toCalendar.setTimeInMillis(prevStreakEndDate);
//
//            Stock stock = new Stock(symbol);
//            List<HistoricalQuote> historyList =
//                   stock.getHistory(fromCalendar, toCalendar, Interval.DAILY);
//
//            //loop through 1 year history to find highest high and low, and prev streak
//            for(int i = 0; i < historyList.size(); i++){
//                HistoricalQuote history = historyList.get(i);
//                boolean resetHistoryStreak = false;
//
//                if(i + 1 < historyList.size()) {
//                    float historyAdjClose = history.getAdjClose().floatValue();
//                    float prevHistoryAdjClose = historyList.get(i + 1).getAdjClose().floatValue();
//
//                    if (historyAdjClose > prevHistoryAdjClose) {
//                        // Down streak broken so break;
//                        if (historyStreak < 0) {
//                            resetHistoryStreak = true;
//                        } else {
//                            historyStreak++;
//                        }
//                    } else if (historyAdjClose < prevHistoryAdjClose) {
//                        // Up streak broken so break;
//                        if (historyStreak > 0) {
//                            resetHistoryStreak = true;
//                        } else {
//                            historyStreak--;
//                        }
//                    }
//                }
//
//                if(resetHistoryStreak){
//                    //record the first history streak as the prev streak
//                    if(prevStreak == 0) {
//                        prevStreak = historyStreak;
//                    }
//                    //set the history high and lows if historyStreak exceeds them
//                    if(historyStreak > yearStreakHigh){
//                        yearStreakHigh = historyStreak;
//                    } else if (historyStreak < yearStreakLow){
//                        yearStreakLow = historyStreak;
//                    }
//
//                      //reset historyStreak to whatever broke the streak so we don't skip it
//                        if(historyStreak > 0){
//                            historyStreak = -1;
//                        }else{
//                            historyStreak = 1;
//                        }
//                }
//            }
//            values = new ContentValues();
//            values.put(StockEntry.COLUMN_PREV_STREAK, prevStreak);
//            values.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, yearStreakHigh);
//            values.put(StockEntry.COLUMN_STREAK_YEAR_LOW, yearStreakLow);
//
//        }finally {
//            if (cursor != null){
//                cursor.close();
//            }
//        }
//
//        return values;
//    }
}