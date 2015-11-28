package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.utils.Utility;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
    private static final String TIMEZONE_NEW_YORK = "America/New_York";

    public NetworkService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String query = intent.getStringExtra(KEY_SEARCH_QUERY);

        try {
            // check for internet
            if(!Utility.isNetworkAvailable(this)){
                showToast(getString(R.string.toast_no_network));
                return;
            }
            String action = intent.getAction();

            switch(action) {
                case ACTION_STOCK_WITH_SYMBOL:
                    ContentValues values = calculateMainInfo(query);

                    if(values == null){
                        return;
                    }
                    // Put stock into the database
                    getContentResolver().insert(StockEntry.buildUri(query), values);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showToast(getString(R.string.toast_error_retrieving_data));
        }
    }

    private ContentValues calculateMainInfo(String symbol) throws IOException{
        int streak = 0;
        long prevStreakEndDate = 0;
        float prevStreakEndPrice = 0;
        float recentClose = 0;
        ContentValues values = null;

        // Query the stock from Yahoo
        Stock stock = YahooFinance.get(symbol);

        if(stock == null){
            showToast(getString(R.string.toast_error_retrieving_data));

        } else if (stock.getName().equals(NOT_AVAILABLE) || (!stock.getStockExchange().equals(NASDAQ)
                && !stock.getStockExchange().equals(NYSE))) {
            showToast(getString(R.string.toast_symbol_not_found));
        } else{
//            Log.d(TAG, "Symbol: " + stock.getSymbol()
//                    + " Full name: " + stock.getName()
//                    + " Exchange: " + stock.getStockExchange()
//                    + " Prev. Close: " + stock.getQuote().getPreviousClose()
//                    + " Open: " + stock.getQuote().getOpen()
//                    + " Current Stock Price: " + stock.getQuote().getPrice()
//                    + " Change $: " + stock.getQuote().getChange()
//                    + " Change %: " + stock.getQuote().getChangeInPercent());

            Calendar nowCalendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
            Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
            fromCalendar.add(Calendar.DAY_OF_MONTH, -MONTH); // We want 1 month of history

            // Download history from Yahoo
            List<HistoricalQuote> historyList =
                    stock.getHistory(fromCalendar, nowCalendar, Interval.DAILY);

            StockQuote quote = stock.getQuote();
            Calendar lastTradeDate = Utility.calendarTimeReset(quote.getLastTradeTime());
            Calendar firstHistoricalDate = Utility.calendarTimeReset(historyList.get(0).getDate());

            if(!lastTradeDate.equals(firstHistoricalDate)){
                Log.d(TAG, "quote");
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
            values.put(StockEntry.COLUMN_SYMBOL, symbol);
            values.put(StockEntry.COLUMN_FULL_NAME, stock.getName());
            values.put(StockEntry.COLUMN_RECENT_CLOSE, recentClose);
            values.put(StockEntry.COLUMN_STREAK, streak);
            values.put(StockEntry.COLUMN_CHANGE_DOLLAR, (float)changeDollarAndPercentage.first);
            values.put(StockEntry.COLUMN_CHANGE_PERCENT, (float)changeDollarAndPercentage.second);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_PRICE, prevStreakEndPrice);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_DATE, prevStreakEndDate);
        }

        return values;
    }

    private ContentValues calculateDetailInfo(String symbol) throws IOException{
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

            cursor = getContentResolver().query(
                    StockContract.StockEntry.buildUri(symbol),
                    projection,
                    null,
                    null,
                    null);

            if (cursor != null && !cursor.moveToFirst()) {
                prevStreakEndDate = cursor.getLong(indexPrevStreakEndDate);
                int streak = cursor.getInt(indexStreak);

                //set its current streak to yearHigh or yearLow
                if(streak > 0 ){
                    yearStreakHigh = streak;
                } else if (streak < 0){
                    yearStreakLow = streak;
                }
            }

            Calendar fromCalendar = Calendar.getInstance();
            fromCalendar.add(Calendar.DAY_OF_MONTH, -YEAR); //We want 1 year of history
            Calendar toCalendar = Calendar.getInstance();
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

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NetworkService.this, toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
