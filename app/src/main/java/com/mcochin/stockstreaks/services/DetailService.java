package com.mcochin.stockstreaks.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.utils.Utility;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.PriorityQueue;

import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Service that goes online to retrieve our information for our detail fragment!
 */
public class DetailService extends Service {
    private static final String TAG = DetailService.class.getSimpleName();
    public static final String KEY_DETAIL_SYMBOL = "detailSymbol";

    private static final int YEAR = 366;

    PriorityQueue<String> q = new PriorityQueue<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        q.clear();

        String symbol = intent.getStringExtra(KEY_DETAIL_SYMBOL);
        q.offer(symbol);

        new AsyncTask<String, Void, Void>(){
            @Override
            protected Void doInBackground(String... params) {
                try {
                    performActionLoadDetails(params[0]);

                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(!q.isEmpty()){
                    execute(q.poll());
                }else{
                    stopSelf();
                }
            }
        }.execute(q.poll());

        return super.onStartCommand(intent, flags, startId);
    }

    private void performActionLoadDetails(String symbol)throws IOException {
        ContentValues values = getDetailValues(symbol);
        getContentResolver().update(StockContract.StockEntry.buildUri(symbol), values, null, null);
    }

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

            // Get streak end date and streak from the specified symbol
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
                Log.d(TAG, prevStreak + " " + yearStreakHigh + " " + yearStreakLow);
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
