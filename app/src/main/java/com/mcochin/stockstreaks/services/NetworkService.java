package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
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

                // Calculate the stock's current streak, prevClose, absolute day coverage and
                // previous streak end price
                StreakInfo streakInfo = calculateStreakRelatedInfo(stock);
                Log.d(TAG, "streak " + streakInfo.mStreak
                    + " recentClose " + streakInfo.mRecentClose
                    + " absoluteDayCoverage " + streakInfo.mStreakAbsoluteDayCoverage
                    + " prevStreakEndPrice " + streakInfo.mPrevStreakEndPrice);

                Pair changeDollarAndPercentage =
                        calculateChange(streakInfo.mRecentClose, streakInfo.mPrevStreakEndPrice);

                Log.d(TAG, "change dollar " + String.format("%.2f", changeDollarAndPercentage.first)
                        + " change percent  " + String.format("%.2f", changeDollarAndPercentage.second));

                ContentValues values = new ContentValues();
                values.put(StockEntry.COLUMN_SYMBOL, stock.getSymbol());
                values.put(StockEntry.COLUMN_FULL_NAME, stock.getName());
                values.put(StockEntry.COLUMN_STREAK, streakInfo.mStreak);
                values.put(StockEntry.COLUMN_STREAK_ABSOLUTE_DAY_COVERAGE, streakInfo.mStreakAbsoluteDayCoverage);
                values.put(StockEntry.COLUMN_RECENT_CLOSE, streakInfo.mRecentClose);
                values.put(StockEntry.COLUMN_CHANGE_DOLLAR, (float)changeDollarAndPercentage.first);
                values.put(StockEntry.COLUMN_CHANGE_PERCENT, (float)changeDollarAndPercentage.second);

               // Put stock into the database
                getContentResolver().insert(StockEntry.buildUri(stock.getSymbol()), values);
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showToast(getString(R.string.toast_error_retrieving_data));
        }
    }

    /**
     * Calculates the stock's current streak, recentClose, absolute day coverage, and prev. streak's
     * end price.
     * @param stock Stock to calculate the info for.
     * @return {@link com.mcochin.stockstreaks.services.NetworkService.StreakInfo}
     * @throws IOException
     */
    private StreakInfo calculateStreakRelatedInfo(Stock stock) throws IOException{
        int streak = 0;
        int streakAbsoluteDayCoverage = 0;
        float prevStreakEndPrice = 0;
        float recentClose = 0;

        Calendar nowTime = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
        Calendar fromTime = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_NEW_YORK));
        //We want historical data for the past month
        fromTime.add(Calendar.DAY_OF_MONTH, -MONTH);

        //network call to download history
        List<HistoricalQuote> historyList = stock.getHistory(fromTime, nowTime, Interval.DAILY);

        //Configure regular hours start and end times
        Calendar regHoursStartTime = Calendar.getInstance();
        regHoursStartTime.set(Calendar.HOUR_OF_DAY, REG_HOURS_START_HOUR);
        regHoursStartTime.set(Calendar.MINUTE, REG_HOURS_START_MINUTE);

        Calendar regHoursEndTime = Calendar.getInstance();
        regHoursEndTime.set(Calendar.HOUR_OF_DAY, REG_HOURS_END_HOUR);
        regHoursEndTime.set(Calendar.MINUTE, REG_HOURS_END_MINUTE);

        // Use quote price first if it is not a active trade day (weekends, holidays) or if is
        // outside reg hours trade time.
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
            recentClose = stock.getQuote().getPrice().floatValue();
        }

        for(int i = 0; i < historyList.size(); i++){
            HistoricalQuote history = historyList.get(i);

            Log.d(TAG, "Date: " + history.getDate().get(Calendar.MONTH)
                    + history.getDate().get(Calendar.DAY_OF_MONTH)
                    + history.getDate().get(Calendar.YEAR)
                    + " Close: " + history.getClose()
                    + " Adjusted close: " + history.getAdjClose());

            // Make sure quote lastTradeTime date doesn't match any history dates so it doesn't
            // get calculated twice. This can happen after active trading hours, when historical
            // data gets updated and the day is an active trading day.
            if(history.getDate().get(Calendar.DAY_OF_MONTH) == lastTradeDay){
                continue;
            }

            // Retrieves recent close if not already retrieved.
            if(recentClose == 0){
                recentClose = history.getClose().floatValue();
            }

            // Need to compare history close to its previous history close. Can't compare to
            // it's open value because stock change values don't get calculated from that.
            // If its the last day in the history we need to skip it because we have nothing to
            // compare it to unless we have access to its ipo price, which we don't.
            if(i + 1 < historyList.size()) {
                float historyClose = history.getClose().floatValue();
                float prevHistoryClose = historyList.get(i+1).getClose().floatValue();
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
        return new StreakInfo(streak, recentClose, streakAbsoluteDayCoverage, prevStreakEndPrice);
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

    private static class StreakInfo {
        public final int mStreak;
        public final float mRecentClose;
        public final int mStreakAbsoluteDayCoverage;
        public final float mPrevStreakEndPrice;

        public StreakInfo(int streak, float recentClose, int streakAbsoluteDayCoverage,
                          float prevStreakEndPrice){
            mStreak = streak;
            mRecentClose = recentClose;
            mStreakAbsoluteDayCoverage = streakAbsoluteDayCoverage;
            mPrevStreakEndPrice = prevStreakEndPrice;
        }
    }
}
