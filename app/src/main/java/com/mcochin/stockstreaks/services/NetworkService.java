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
import com.mcochin.stockstreaks.data.StockContract.UpdateDateEntry;
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
    public static final String ACTION_STOCKS = "actionStocks";
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
        String query = intent.getStringExtra(KEY_SEARCH_QUERY);

        try {
            // check for internet
            if(!Utility.isNetworkAvailable(this)){
                Utility.showToast(this, getString(R.string.toast_no_network));
                return;
            }
            String action = intent.getAction();

            switch(action) {
                case ACTION_STOCKS:
                    performActionStocks();
                    break;

                case ACTION_STOCK_WITH_SYMBOL:
                    performActionStockWithSymbol(query);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Utility.showToast(this, getString(R.string.toast_error_retrieving_data));
        }
    }

    private void performActionStocks() throws IOException{
        //Check if you can update first
        if(!Utility.canUpdateList(getContentResolver())){
            Utility.showToast(this, getString(R.string.toast_already_up_to_date));
            return;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        Map<String, Stock> stockList = fetchStockList();
        if(stockList != null) {
            for (Stock stock : stockList.values()) {
                ContentValues values = getMainValues(stock);
                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                        .withValues(values)
                        .withYieldAllowed(true)
                        .build());
            }
            try {
                getContentResolver().applyBatch(StockContract.CONTENT_AUTHORITY, ops);
            }catch (RemoteException | OperationApplicationException e){
                Log.e(TAG, Log.getStackTraceString(e));
                Utility.showToast(this, getString(R.string.toast_error_updating_list));
            }

            updateUpdateDate();
        }
    }

    private void performActionStockWithSymbol(String symbol)throws IOException{
        // Check if symbol already exists in database
        if (Utility.isEntryExist(symbol, getContentResolver())) {
            Utility.showToast(this, getString(R.string.toast_symbol_exists));
            return;
        }

        Stock stock = fetchStockItem(symbol);
        ContentValues values = getMainValues(stock);

        if(values == null){
            return;
        }
        // Put stock into the database
        getContentResolver().insert(StockEntry.buildUri(symbol), values);
    }

    private Map<String, Stock> fetchStockList() throws IOException{
        Cursor cursor = null;
        try{
            final String[] projection = new String[] {StockEntry.COLUMN_SYMBOL};
            final int indexSymbol = 0;

            // Get all symbols in the table
            cursor = getContentResolver().query(StockEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

            if(cursor != null){
                int cursorCount = cursor.getCount();
                if(cursorCount == 0){
                    return null;
                }

                String[] symbolsList = new String[cursorCount];

                int i = 0;
                while(cursor.moveToNext()){
                    symbolsList[i] = cursor.getString(indexSymbol);
                    i++;
                }

                return YahooFinance.get(symbolsList);
            }

        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    private Stock fetchStockItem(String symbol) throws IOException{
        // Query the stock from Yahoo
        return YahooFinance.get(symbol);
    }

    private ContentValues getMainValues(Stock stock) throws IOException{
        int streak = 0;
        long prevStreakEndDate = 0;
        float prevStreakEndPrice = 0;
        float recentClose = 0;
        ContentValues values = null;

        if(stock == null){
            Utility.showToast(this, getString(R.string.toast_error_retrieving_data));

        } else if (stock.getName().equals(NOT_AVAILABLE) || (!stock.getStockExchange().equals(NASDAQ)
                && !stock.getStockExchange().equals(NYSE))) {
            Utility.showToast(this, getString(R.string.toast_symbol_not_found));
        } else{
//            Log.d(TAG, "Symbol: " + stock.getSymbol()
//                    + " Full name: " + stock.getName()
//                    + " Exchange: " + stock.getStockExchange()
//                    + " Prev. Close: " + stock.getQuote().getPreviousClose()
//                    + " Open: " + stock.getQuote().getOpen()
//                    + " Current Stock Price: " + stock.getQuote().getPrice()
//                    + " Change $: " + stock.getQuote().getChange()
//                    + " Change %: " + stock.getQuote().getChangeInPercent());

            // Get history from a month ago to today!
            Calendar nowCalendar = Utility.getNewYorkCalendarInstance();
            Calendar fromCalendar = Utility.getNewYorkCalendarInstance();
            fromCalendar.add(Calendar.DAY_OF_MONTH, -MONTH);

            // Download history from Yahoo
            List<HistoricalQuote> historyList =
                    stock.getHistory(fromCalendar, nowCalendar, Interval.DAILY);


            StockQuote quote = stock.getQuote();
            Calendar lastTradeDate = Utility.calendarTimeReset(quote.getLastTradeTime());
            Calendar firstHistoricalDate = Utility.calendarTimeReset(historyList.get(0).getDate());

            // "nowTimeDay > lastTradeDay" will cover holidays and weekends in which history has
            // not updated yet!
            int nowTimeDay = nowCalendar.get(Calendar.DAY_OF_MONTH);
            int lastTradeDay = lastTradeDate.get(Calendar.DAY_OF_MONTH);

            if(!lastTradeDate.equals(firstHistoricalDate)
                    && (nowTimeDay > lastTradeDay || !Utility.isDuringTradingHours())){
                Log.d(TAG, "using stock price");
                if (quote.getChange().floatValue() > 0) {
                    streak++;

                } else if (quote.getChange().floatValue() < 0) {
                    streak--;
                }
                recentClose = stock.getQuote().getPrice().floatValue();
            }

            for (int i = 0; i < historyList.size(); i++) {
                HistoricalQuote history = historyList.get(i);

//                Log.d(TAG, "Date: " + history.getDate().get(Calendar.MONTH)
//                        + history.getDate().get(Calendar.DAY_OF_MONTH)
//                        + history.getDate().get(Calendar.YEAR)
//                        + " Hour: " + history.getDate().get(Calendar.HOUR_OF_DAY)
//                        + " Minute: " + history.getDate().get(Calendar.MINUTE)
//                        + " Close: " + history.getClose()
//                        + " Adjusted close: " + history.getAdjClose());

                if (recentClose == 0) {
                    // Retrieves most recent close if not already retrieved.
                    recentClose = history.getAdjClose().floatValue();
                }

                float historyAdjClose = history.getAdjClose().floatValue();
                boolean shouldBreak = false;

                // Need to compare history adj close to its previous history's adj close.
                // http://budgeting.thenest.com/adjusted-closing-price-vs-closing-price-32457.html
                // If its the last day in the history we need to skip it because we have
                // nothing to compare it to.
                if (i + 1 < historyList.size()) {
                    float prevHistoryAdjClose = historyList.get(i + 1).getAdjClose().floatValue();

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

//            Log.d(TAG, "streak " + streak
//                    + " recentClose " + recentClose
//                    + " prevStreakEndDate " + prevStreakEndDate
//                    + " prevStreakEndPrice " + prevStreakEndPrice);

            Pair changeDollarAndPercentage =
                    calculateChange(recentClose, prevStreakEndPrice);

//            Log.d(TAG, "change dollar " + String.format("%.2f", changeDollarAndPercentage.first)
//                    + " change percent  " + String.format("%.2f", changeDollarAndPercentage.second));

            values = new ContentValues();
            values.put(StockEntry.COLUMN_SYMBOL, stock.getSymbol());
            values.put(StockEntry.COLUMN_FULL_NAME, stock.getName());
            values.put(StockEntry.COLUMN_RECENT_CLOSE, recentClose);
            values.put(StockEntry.COLUMN_STREAK, streak);
            values.put(StockEntry.COLUMN_CHANGE_DOLLAR, (float)changeDollarAndPercentage.first);
            values.put(StockEntry.COLUMN_CHANGE_PERCENT, (float)changeDollarAndPercentage.second);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_PRICE, prevStreakEndPrice);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_DATE, prevStreakEndDate);
            values.put(StockEntry.COLUMN_PREV_STREAK, 0);
            values.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, 0);
            values.put(StockEntry.COLUMN_STREAK_YEAR_LOW, 0);
        }

        return values;
    }

    private ContentValues getDetailValues(String symbol) throws IOException{
        Cursor cursor = null;
        int historyStreak = 0;
        int prevStreak = 0;
        int yearStreakHigh = 0;
        int yearStreakLow = 0;

        ContentValues values;

        try {
            //projection
            final String[] projection = new String[]{
                    StockEntry.COLUMN_PREV_STREAK_END_DATE,
                    StockEntry.COLUMN_STREAK
            };

            //indexes for the projection
            final int indexPrevStreakEndDate = 0;
            final int indexStreak = 1;

            long prevStreakEndDate = 0;

            // Get streak end date and streak from the specified symbol
            cursor = getContentResolver().query(
                    StockContract.StockEntry.buildUri(symbol),
                    projection,
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                prevStreakEndDate = cursor.getLong(indexPrevStreakEndDate);
                int streak = cursor.getInt(indexStreak);

                //set its current streak to yearHigh or yearLow
                if(streak > 0 ){
                    yearStreakHigh = streak;
                } else if (streak < 0){
                    yearStreakLow = streak;
                }
            }

            Calendar fromCalendar = Utility.getNewYorkCalendarInstance();
            fromCalendar.add(Calendar.DAY_OF_MONTH, -YEAR); //We want 1 year of history
            Calendar toCalendar = Utility.getNewYorkCalendarInstance();
            toCalendar.setTimeInMillis(prevStreakEndDate);

            Stock stock = new Stock(symbol);
            List<HistoricalQuote> historyList =
                   stock.getHistory(fromCalendar, toCalendar, Interval.DAILY);

            //loop through 1 year history to find highest high and low, and prev streak
            for(int i = 0; i < historyList.size(); i++){
                HistoricalQuote history = historyList.get(i);
                boolean resetHistoryStreak = false;

                if(i + 1 < historyList.size()) {
                    float historyAdjClose = history.getAdjClose().floatValue();
                    float prevHistoryAdjClose = historyList.get(i + 1).getAdjClose().floatValue();

                    if (historyAdjClose > prevHistoryAdjClose) {
                        // Down streak broken so break;
                        if (historyStreak < 0) {
                            resetHistoryStreak = true;
                        } else {
                            historyStreak++;
                        }
                    } else if (historyAdjClose < prevHistoryAdjClose) {
                        // Up streak broken so break;
                        if (historyStreak > 0) {
                            resetHistoryStreak = true;
                        } else {
                            historyStreak--;
                        }
                    }
                }

                if(resetHistoryStreak){
                    //record the first history streak as the prev streak
                    if(prevStreak == 0) {
                        prevStreak = historyStreak;
                    }
                    //set the history high and lows if historyStreak exceeds them
                    if(historyStreak > yearStreakHigh){
                        yearStreakHigh = historyStreak;
                    } else if (historyStreak < yearStreakLow){
                        yearStreakLow = historyStreak;
                    }

                    //reset historyStreak
                    historyStreak = 0;
                }
            }
            values = new ContentValues();
            values.put(StockEntry.COLUMN_PREV_STREAK, prevStreak);
            values.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, yearStreakHigh);
            values.put(StockEntry.COLUMN_STREAK_YEAR_LOW, yearStreakLow);

        }finally {
            if (cursor != null){
                cursor.close();
            }
        }

        return values;
    }

    private ContentValues getUpdateDateValues(){
        ContentValues values = new ContentValues();
        values.put(UpdateDateEntry.COLUMN_TIME_IN_MILLI, System.currentTimeMillis());

        return values;
    }

    private void updateUpdateDate(){
        //Update updateDate if exists, if not insert
        ContentValues updateDateValues = getUpdateDateValues();
        int rowsAffected = getContentResolver().update(
                UpdateDateEntry.CONTENT_URI,
                updateDateValues,
                null,
                null);
        if(rowsAffected == 0){
            getContentResolver().insert(UpdateDateEntry.CONTENT_URI, updateDateValues);
        }
    }

    /**
     * Calculates the change in dollars and percentage between the two prices.
     * @param recentClose Stock's recent close
     * @param prevStreakEndPrice Previous streak's end price for the stock
     * @return A Pair containing:
     * <ul>
     *     <li>Pair.first is the change in dollars</li>
     *     <li>Pair.second is the change in percentage</li>
     * </ul>
     */
    private Pair<Float, Float> calculateChange(float recentClose, float prevStreakEndPrice){
        float changeDollar = recentClose - prevStreakEndPrice;
        float changePercent = changeDollar / prevStreakEndPrice * 100;

        return new Pair<>(changeDollar, changePercent);
    }
}
