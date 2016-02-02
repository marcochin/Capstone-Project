package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.custom.MyApplication;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.events.AppRefreshFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.LoadMoreFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.data.ListEventQueue;
import com.mcochin.stockstreaks.pojos.events.WidgetRefreshDelegateEvent;
import com.mcochin.stockstreaks.utils.Utility;
import com.mcochin.stockstreaks.widget.StockWidgetProvider;
import com.mcochin.stockstreaks.widget.StockWidgetRemoteViewsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
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
    public static final String KEY_LOAD_MORE_QUERY ="loadMoreQuery";
    public static final String KEY_SESSION_ID ="sessionId";

    public static final String ACTION_LOAD_MORE = "actionLoadMore";
    public static final String ACTION_LOAD_SYMBOL = "actionStockWithSymbol";
    public static final String ACTION_APP_REFRESH ="actionAppRefresh";
    public static final String ACTION_WIDGET_REFRESH ="actionWidgetRefresh";

    //needs to be 32 since we need to compare closing to prev day's closing price
    private static final int MONTH = 32;

    private static final String NOT_AVAILABLE = "N/A";
    private static final String USD = "USD";
    private static final String NASDAQ = "NMS";
    private static final String NYSE = "NYQ";

    public MainService(){
        super(TAG);
    }

    private String mSessionId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Every request should have a session id. The sessionId may not exist yet if the process
        // was started by the widget and hence we have to create a new session if so.
        if(MyApplication.getInstance().getSessionId().isEmpty()){
            MyApplication.startNewSession();
        }
        intent.putExtra(KEY_SESSION_ID, MyApplication.getInstance().getSessionId());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mSessionId = intent.getStringExtra(KEY_SESSION_ID);
        String action = intent.getAction();
        try {
            // check if the session is still valid
            if(!MyApplication.validateSessionId(mSessionId)){
                throw new IllegalStateException();
            }

            // check for internet
            if(!Utility.isNetworkAvailable(this)){
                throw new IOException(getString(R.string.toast_no_network));
            }

            switch(action) {
                case ACTION_LOAD_SYMBOL:
                    String symbol = intent.getStringExtra(KEY_LOAD_SYMBOL_QUERY);
                    performActionLoadSymbol(symbol);
                    break;

                case ACTION_LOAD_MORE:
                    String[] symbols = intent.getStringArrayExtra(KEY_LOAD_MORE_QUERY);
                    performActionLoadMore(symbols);
                    break;

                case ACTION_APP_REFRESH:
                    performActionAppRefresh();
                    break;

                case ACTION_WIDGET_REFRESH:
                    performActionWidgetRefresh();
                    break;
            }
        } catch (IOException | IllegalArgumentException| IllegalStateException e) {
            Log.e(TAG, Log.getStackTraceString(e));

            if(e instanceof IllegalArgumentException){
                Utility.showToast(this, e.getMessage());

            }else if(e instanceof IOException){
                if(e.getMessage().equals(getString(R.string.toast_no_network))) {
                    Utility.showToast(this, e.getMessage());
                }
                else{
                    Utility.showToast(this, getString(R.string.toast_error_retrieving_data));
                }
            }

            switch(action) {
                case ACTION_LOAD_SYMBOL:
                    ListEventQueue.getInstance().post(new LoadSymbolFinishedEvent(
                            mSessionId, null, false));
                    break;

                case ACTION_LOAD_MORE:
                    ListEventQueue.getInstance().post(new LoadMoreFinishedEvent(
                            mSessionId, null, false));
                    break;

                case ACTION_APP_REFRESH:
                case ACTION_WIDGET_REFRESH:
                    ListEventQueue.getInstance().post(new AppRefreshFinishedEvent(
                            mSessionId, null, false));
                    sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATE_ERROR));
                    MyApplication.getInstance().setRefreshing(false);
                    break;
            }
        }
    }

    private void performActionLoadSymbol(String symbol)throws IOException{
        // Check if symbol already exists in database
        if (isEntryExist(symbol)) {
            throw new IllegalArgumentException(getString(R.string.toast_placeholder_symbol_exists, symbol));
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Add update time operation to list
        ops.add(getUpdateTimeOperation());

        Stock stock = YahooFinance.get(symbol);
        ContentValues values = getMainValues(stock);

        // Add insert operation to list
        ops.add(ContentProviderOperation
                .newInsert(StockEntry.buildUri(stock.getSymbol()))
                .withValues(values)
                .withYieldAllowed(true)
                .build());

        applyOperations(ops, StockProvider.METHOD_LOAD_SYMBOL, null);
    }

    private void performActionLoadMore(String[] symbolsToLoad) throws IOException{
        ArrayList<ContentProviderOperation> ops = getLoadMoreOperations(symbolsToLoad);
        applyOperations(ops, StockProvider.METHOD_LOAD_A_FEW, null);
    }

    private void performActionAppRefresh()throws IOException{
        if(!refreshList(false)){
            ListEventQueue.getInstance().post(new AppRefreshFinishedEvent(mSessionId, null, false));
            sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATE_ERROR));
        }
    }

    private void performActionWidgetRefresh()throws IOException{
        // If we receive a msg that widget is refreshing and the user is currently using the
        // app, delegate the refresh to the app
        if(EventBus.getDefault().hasSubscriberForEvent(WidgetRefreshDelegateEvent.class)){
            ListEventQueue.getInstance().post(new WidgetRefreshDelegateEvent(mSessionId));

        }else if(!refreshList(true)){
            ListEventQueue.getInstance().post(new AppRefreshFinishedEvent(mSessionId, null, false));
            sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATE_ERROR));
        }
    }

    /**
     * @param refreshingFromWidget set to true if only the widget is refreshing, false otherwise
     * @return true there are items to be refreshed, false if there are no items in the list
     * @throws IOException
     */
    private boolean refreshList(boolean refreshingFromWidget)throws IOException{
        Cursor cursor = null;
        try {
            final String[] projection = new String[]{StockEntry.COLUMN_SYMBOL};
            final int indexSymbol = 0;

            // Widget does not have the ability to add more to its list, so we need to load a little
            // bit more on its refresh to make it more useful.
            int more = refreshingFromWidget? StockWidgetRemoteViewsService.MORE : ListManipulator.MORE;

            // Query db for the FIRST FEW as a normal refresh would do.
            cursor = getContentResolver().query(
                    StockEntry.CONTENT_URI,
                    projection,
                    StockProvider.LIST_POSITION_SELECTION,
                    new String[]{Integer.toString(more)},
                    StockProvider.ORDER_BY_LIST_POSITION_ASC_ID_DESC);

            if (cursor != null) {
                int cursorCount = cursor.getCount();
                String[] symbolsToLoad = new String[cursorCount];

                for (int i = 0; i < cursorCount; i++) {
                    cursor.moveToPosition(i);
                    symbolsToLoad[i] = cursor.getString(indexSymbol);
                }
                if (cursorCount != 0) {
                    ContentProviderOperation updateTimeOp = getUpdateTimeOperation();
                    ArrayList<ContentProviderOperation> ops = getLoadMoreOperations(symbolsToLoad);

                    ops.add(updateTimeOp);
                    applyOperations(ops, StockProvider.METHOD_REFRESH, null);
                    return true;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        MyApplication.getInstance().setRefreshing(false);
        return false;
    }

    /**
     * Calculates the stock's values that is shown in the list's item view.
     * @param stock
     * @return
     * @throws IOException
     */
    private ContentValues getMainValues(Stock stock) throws IOException{
        float recentClose = 0;
        ContentValues values;

        // check if the session is still valid
        if(!MyApplication.validateSessionId(mSessionId)){
            throw new IllegalStateException();
        }

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
            // "nowTimeDay > lastTradeDay" covers time during "non-active" trade hours such as
            // holidays and weekends in which history has not updated yet!
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

    /**
     * Loops through the stock's history to calculate it's current streak and streak info.
     * This only loops through enough of the history to calculate info needed to show in the list
     * item.
     * @param historyList
     * @param recentClose
     * @return
     */
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
        values.put(StockEntry.COLUMN_STREAK_CHART_MAP, "");

        return values;
    }

    /**
     * Queries the network for the symbolsToLoad in one request to retrieve their latest
     * main values.
     * @param symbolsToLoad The symbols to be updated.
     * @return This will return the update operations of the stocks needed to update the db.
     * @throws IOException
     */
    private ArrayList<ContentProviderOperation> getLoadMoreOperations(String[] symbolsToLoad) throws IOException{
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        Map<String, Stock> stockList = YahooFinance.get(symbolsToLoad);
        if (stockList != null) {
            for (String symbol : symbolsToLoad) {
                Stock stock = stockList.get(symbol);
                ContentValues values = getMainValues(stock);

                // Add update operations to list
                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                        .withValues(values)
                        .withYieldAllowed(true)
                        .build());
            }
        }
        // Save the shown list position every time load a few is performed. This is primarily
        // for the widget refreshes, but also for one edge case in which the list updates after
        // onPause() is called and the shown list position will not be reflected on next app
        // open if user exits our app.
        ops.add(getListPositionBookmarkOperation(symbolsToLoad[symbolsToLoad.length - 1]));
        return ops;
    }

    /**
     * Gets an update operation to update the update time.
     * The update time will be updated whenever the list refreshes and whenever a new symbol has
     * been added.
     * @return an update operation to update the update time.
     */
    private ContentProviderOperation getUpdateTimeOperation(){
        ContentValues values = new ContentValues();
        values.put(SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI, System.currentTimeMillis());

        return ContentProviderOperation
                .newUpdate(SaveStateEntry.CONTENT_URI)
                .withValues(values)
                .withYieldAllowed(true)
                .build();
    }

    /**
     * Gets an update operation to update the shown position bookmark to the last shown position
     * bookmark. The shown list position bookmark needs to be updated whenever a the list is
     * refreshed or items are added after load "more".
     * @return an update operation to update the shown position bookmark to the last shown position
     * bookmark.
     */
    private ContentProviderOperation getListPositionBookmarkOperation(String symbol){
        Cursor cursor = null;
        int listPosition = ListManipulator.MORE; //Default to "more" in case something goes wrong
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
        values.put(SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK, listPosition + 1);

        return ContentProviderOperation
                .newUpdate(SaveStateEntry.CONTENT_URI)
                .withValues(values)
                .withYieldAllowed(true)
                .build();
    }

    /**
     * Used to determine is a symbol already exists in the database
     *
     * @param symbol The symbol to look up
     * @return true if exists, otherwise false
     */
    public boolean isEntryExist(String symbol) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(StockEntry.buildUri(symbol), null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                return true;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * Sends the operations to the Content Provider to be executed.
     * @param ops the operations to be executed
     * @param method the custom method of your choice.
     * @param arg
     */
    private void applyOperations(ArrayList<ContentProviderOperation> ops, String method, String arg){
        Bundle extras = new Bundle();
        extras.putParcelableArrayList(StockProvider.KEY_OPERATIONS, ops);
        extras.putString(MainService.KEY_SESSION_ID, mSessionId);

        getContentResolver().call(StockContract.BASE_CONTENT_URI,
                method,
                arg,
                extras);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // If the task was swiped while list was refreshing,
        // send broadcast to hide the progress wheel.
        sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATE_ERROR));
    }
}