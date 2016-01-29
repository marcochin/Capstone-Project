package com.mcochin.stockstreaks.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.custom.MyApplication;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.pojos.events.LoadDetailErrorEvent;
import com.mcochin.stockstreaks.utils.Utility;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.greenrobot.event.EventBus;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Service that goes online to retrieve our information for our detail fragment!
 */
public class DetailService extends Service {
    private static final String TAG = DetailService.class.getSimpleName();
    public static final String KEY_DETAIL_SYMBOL = "detailSymbol";
    public static final String KEY_SESSION_ID ="sessionId";

    //needs to be 366 since we need to compare closing to prev day's closing price
    private static final int YEAR = 366;

    Queue<Intent> mQueue = new LinkedList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mQueue.clear();
        // Every request should have a session id
        intent.putExtra(KEY_SESSION_ID, MyApplication.getInstance().getSessionId());
        mQueue.offer(intent);

        new AsyncTask<Intent, Void, Void>(){
            @Override
            protected Void doInBackground(Intent... params) {
                try {
                    // check for internet
                    if(!Utility.isNetworkAvailable(DetailService.this)){
                        throw new IOException(getString(R.string.toast_no_network));
                    }

                    performActionLoadDetails(params[0].getStringExtra(KEY_DETAIL_SYMBOL));

                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));

                    if(e.getMessage().equals(getString(R.string.toast_no_network))){
                        Utility.showToast(DetailService.this, e.getMessage());
                    }else {
                        Utility.showToast(DetailService.this, getString(R.string.toast_error_retrieving_data));
                    }
                    EventBus.getDefault().post(new LoadDetailErrorEvent(
                            params[0].getStringExtra(KEY_SESSION_ID)));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(!mQueue.isEmpty()){
                    execute(mQueue.poll());
                }else{
                    stopSelf();
                }
            }
        }.execute(mQueue.poll());

        return super.onStartCommand(intent, flags, startId);
    }

    private void performActionLoadDetails(String symbol)throws IOException {
        ContentValues values = getDetailValues(symbol);
        getContentResolver().update(StockContract.StockEntry.buildUri(symbol), values, null, null);
    }

    /**
     * Retrieves the symbol's one year history and calculates its prev. streak, streak's year high,
     * and streak's year low.
     * @param symbol
     * @return
     * @throws IOException
     */
    private ContentValues getDetailValues(String symbol) throws IOException{
        Cursor cursor = null;
        ContentValues values = null;

        try {
            int streakCounter = 0;
            int prevStreak = 0;
            int yearStreakHigh = 0;
            int yearStreakLow = 0;

            //projection
            final String[] projection = new String[]{
                    StockContract.StockEntry.COLUMN_PREV_STREAK_END_DATE,
                    StockContract.StockEntry.COLUMN_PREV_STREAK_END_PRICE,
                    StockContract.StockEntry.COLUMN_STREAK
            };

            //indexes for the projection
            final int indexPrevStreakEndDate = 0;
            final int indexPrevStreakEndPrice = 1;
            final int indexStreak = 2;

            // Get prev streak end date and streak from the specified symbol
            cursor = getContentResolver().query(
                    StockContract.StockEntry.buildUri(symbol),
                    projection,
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                long prevStreakEndDate;
                float prevStreakEndPrice;
                int streak;

                prevStreakEndDate = cursor.getLong(indexPrevStreakEndDate);
                prevStreakEndPrice = cursor.getFloat(indexPrevStreakEndPrice);
                streak = cursor.getInt(indexStreak);

                //set its current streak to yearHigh or yearLow
                if (streak > 0) {
                    yearStreakHigh = streak;
                } else if (streak < 0) {
                    yearStreakLow = streak;
                }

                Calendar fromCalendar = Utility.getNewYorkCalendarInstance();
                fromCalendar.add(Calendar.DAY_OF_MONTH, -YEAR); //We want 1 year of history

                Calendar toCalendar = Utility.getNewYorkCalendarInstance();
                toCalendar.setTimeInMillis(prevStreakEndDate);
                toCalendar.add(Calendar.DAY_OF_MONTH, 1); //Add 1 to for Yahoo's inconsistent offset

                Stock stock = new Stock(symbol);
                List<HistoricalQuote> historyList =
                        stock.getHistory(fromCalendar, toCalendar, Interval.DAILY);

                // Check if the prevStreakEndPrice we retrieved from MainService matches the
                // price of the first history, if not we will skip it. Yahoo's inconsistent offset.
                int startIndex = prevStreakEndPrice ==
                        Utility.roundTo2FloatDecimals(historyList.get(0).getAdjClose().floatValue())
                        ? 0
                        : 1;

                //loop through 1 year history to find highest high and low, and prev streak
                for (int i = startIndex; i < historyList.size(); i++) {
                    HistoricalQuote history = historyList.get(i);
                    boolean resetStreakCounter = false;

                    if (i + 1 < historyList.size()) {
                        float historyAdjClose = history.getAdjClose().floatValue();
                        float prevHistoryAdjClose = historyList.get(i + 1).getAdjClose().floatValue();

                        if (historyAdjClose > prevHistoryAdjClose) {
                            // Down streak broken so break;
                            if (streakCounter < 0) {
                                resetStreakCounter = true;
                            } else {
                                streakCounter++;
                            }
                        } else if (historyAdjClose < prevHistoryAdjClose) {
                            // Up streak broken so break;
                            if (streakCounter > 0) {
                                resetStreakCounter = true;
                            } else {
                                streakCounter--;
                            }
                        }
                    }

                    if (resetStreakCounter) {
                        //record the first history streak as the prev streak
                        if (prevStreak == 0) {
                            prevStreak = streakCounter;
                        }
                        //set the history high and lows if streakCounter exceeds them
                        if (streakCounter > yearStreakHigh) {
                            yearStreakHigh = streakCounter;
                        } else if (streakCounter < yearStreakLow) {
                            yearStreakLow = streakCounter;
                        }

                        //reset streakCounter to whatever broke the streak so we don't skip it
                        if (streakCounter > 0) {
                            streakCounter = -1;
                        } else {
                            streakCounter = 1;
                        }
                    }
                }
                values = new ContentValues();
                values.put(StockContract.StockEntry.COLUMN_PREV_STREAK, prevStreak);
                values.put(StockContract.StockEntry.COLUMN_STREAK_YEAR_HIGH, yearStreakHigh);
                values.put(StockContract.StockEntry.COLUMN_STREAK_YEAR_LOW, yearStreakLow);
            }

        }finally {
            if (cursor != null){
                cursor.close();
            }
        }

        return values;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
