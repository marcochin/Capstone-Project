package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.custom.Triple;
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
    public static String KEY_SEARCH_QUERY ="searchQuery";
    private static String ACTION_ITEM_QUERY = "actionItemQuery";
    private static String ACTION_LIST_QUERY = "actionListQuery";

    private static final int MONTH = 31;
    private static final int YEAR = 365;
    private static final int REG_HOURS_START_HOUR = 9;
    private static final int REG_HOURS_START_MINUTE = 29;
    private static final int REG_HOURS_END_HOUR = 4;
    private static final int REG_HOURS_END_MINUTE = 30;

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

            Stock stock = YahooFinance.get(query);

            if(stock == null){
                showToast(getString(R.string.toast_error_retrieving_data));

            } else {
                if(stock.getName().equals(NOT_AVAILABLE)
                        || (!stock.getStockExchange().equals(NASDAQ)
                        && !stock.getStockExchange().equals(NYSE))){
                    showToast(getString(R.string.toast_symbol_not_found));
                    return;
                }

                Log.d(TAG, "Symbol: " + stock.getSymbol()
                        + " Full name: " + stock.getName()
                        + " Exchange: " + stock.getStockExchange()
                        + " Prev. Close: " + stock.getQuote().getPreviousClose()
                        + " Open: " + stock.getQuote().getOpen()
                        + " Current Stock Price: " + stock.getQuote().getPrice()
                        + " Change $: " + stock.getQuote().getChange()
                        + " Change %: " + stock.getQuote().getChangeInPercent());

                // Calculate the stock's current streak, absolute day coverage and
                // previous streak end price
                Triple stockTriple = calculateStockTriple(stock);
                Log.d(TAG, "streak " + stockTriple.first
                    + " absoluteDayCoverage " + stockTriple.second
                    + " prevStreakEndPrice " + stockTriple.third);

//                ContentValues values = new ContentValues();
//                values.put(StockEntry.COLUMN_SYMBOL, stock.getSymbol());
//                values.put(StockEntry.COLUMN_FULL_NAME, stock.getName());
//                values.put(StockEntry.COLUMN_PREV_CLOSE,
//                        stock.getQuote().getPreviousClose().floatValue());
//                values.put(StockEntry.COLUMN_CHANGE_DOLLAR,
//                        stock.getQuote().getChange().floatValue());
//                values.put(StockEntry.COLUMN_CHANGE_DOLLAR,
//                        stock.getQuote().getChangeInPercent().floatValue());
//                //values.put(StockEntry.COLUMN_STREAK, streak);
//
//
//                // TODO put stock into the database
//                getContentResolver().insert(
//                        StockContract.StockEntry.buildUri(stock.getSymbol()), values);

            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showToast(getString(R.string.toast_error_retrieving_data));
        }
    }

    /**
     *
     * @param stock Stock to calculate the Triple for.
     * @return A Triple containing the following:
     * <ul>
     * <li>Triple.first is the streak</li>
     * <li>Triple.second is the streak's absolute day coverage</li>
     * <li>Triple.third is the prev streak's end price</li>
     * </ul>
     * @throws IOException
     */
    private Triple<Integer, Integer, Float> calculateStockTriple(Stock stock) throws IOException{
        int streak = 0;
        int streakAbsoluteDayCoverage = 0;
        float prevStreakEndPrice = 0;

        Calendar nowTime = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
        Calendar fromTime = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
        //We want historical data for the past month
        fromTime.add(Calendar.DAY_OF_MONTH, -MONTH);

        //network call to download history
        List<HistoricalQuote> history = stock.getHistory(fromTime, nowTime, Interval.DAILY);

        //Configure regular hours start and end times
        Calendar regHoursStartTime = Calendar.getInstance();
        regHoursStartTime.set(Calendar.HOUR_OF_DAY, REG_HOURS_START_HOUR);
        regHoursStartTime.set(Calendar.MINUTE, REG_HOURS_START_MINUTE);

        Calendar regHoursEndTime = Calendar.getInstance();
        regHoursEndTime.set(Calendar.HOUR_OF_DAY, REG_HOURS_END_HOUR);
        regHoursEndTime.set(Calendar.MINUTE, REG_HOURS_END_MINUTE);

        // Use quote price if it is not a active trade day (weekends, holidays) or if is outside
        // reg hours trade time.
        // Basically, we just use history only approach during reg trading hours.
        // Days with no price change have no effect on streaks, but it will add toward
        // streakAbsoluteDayCoverage
        StockQuote quote = stock.getQuote();
        int lastTradeDay = quote.getLastTradeTime().get(Calendar.DAY_OF_MONTH);

        if(nowTime.get(Calendar.DAY_OF_MONTH) != lastTradeDay
                || !(nowTime.after(regHoursStartTime) && nowTime.before(regHoursEndTime))) {
            Log.d(TAG, "quote");

            if (quote.getChange().floatValue() > 0) {
                streak++;

            } else if (quote.getChange().floatValue() < 0) {
                streak--;
            }
            //increase absolute day coverage whether stock is up or down or no change
            streakAbsoluteDayCoverage++;
        }

        for(int i = 0; i < history.size(); i++){
            Log.d(TAG, "Date: " + history.get(i).getDate().get(Calendar.MONTH)
                    + history.get(i).getDate().get(Calendar.DAY_OF_MONTH)
                    + history.get(i).getDate().get(Calendar.YEAR)
                    + " Close: " + history.get(i).getClose()
                    + " Adjusted close: " + history.get(i).getAdjClose());

            // Make sure quote lastTradeTime date doesn't match any history dates so it doesn't
            // get calculated twice. This can happen after active trading hours, when historical
            // data gets updated and the day is an active trading day.
            if(history.get(i).getDate().get(Calendar.DAY_OF_MONTH) == lastTradeDay){
                continue;
            }

            // Need to compare history close to its previous history close. Can't compare to
            // it's open value because stock change values don't get calculated from that.
            // If its the last day in the history we need to skip it because we have nothing to
            // compare it to unless we have access to its ipo price, which we don't.
            if(i + 1 < history.size()) {
                float historyClose = history.get(i).getClose().floatValue();
                float prevHistoryClose = history.get(i+1).getClose().floatValue();
                boolean shouldBreak = false;

                if (historyClose > prevHistoryClose){
                    // Down streak broken so break;
                    if(streak < 0){
                        shouldBreak = true;
                    }else {
                        streak++;
                    }
                } else if (historyClose < prevHistoryClose){
                    // Up streak broken so break;
                    if(streak > 0){
                        shouldBreak = true;
                    }else {
                        streak--;
                    }
                }

                if(shouldBreak){
                    prevStreakEndPrice = historyClose;
                    break;
                }

                //increase absolute day coverage whether stock is up or down or no change
                streakAbsoluteDayCoverage++;
            }
        }
        return new Triple<>(streak, streakAbsoluteDayCoverage, prevStreakEndPrice);
    }

    private Pair<Float, Float> calculateChange(float currentPrice, float prevStreakEndPrice){
        return null;
    }

    private void calculateDetailInfo(Stock stock){

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
