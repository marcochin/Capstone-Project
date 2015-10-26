package com.mcochin.stockstreakz.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.mcochin.stockstreakz.MainActivity;
import com.mcochin.stockstreakz.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

/**
 * Service that goes online to retrieve our information!
 */
public class NetworkService extends IntentService {
    private static final String TAG = NetworkService.class.getSimpleName();
//    private static String SEARCH_REQUEST = NetworkService.class.getSimpleName();
//    private static String LIST_REFRESH_REQUEST = NetworkService.class.getSimpleName();
    private static final int MONTH = 31;

    public NetworkService(){
        super("NetworkService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String query = intent.getStringExtra(MainActivity.SEARCH_VIEW_QUERY);

        Calendar toCalendar = Calendar.getInstance();
        Calendar fromCalendar = Calendar.getInstance();
        //We want historical data for the past month
        fromCalendar.add(Calendar.DAY_OF_MONTH, -MONTH);

        try {
            Stock stock = YahooFinance.get(query, fromCalendar, toCalendar, Interval.DAILY);
            if(stock == null){
                showToast(getString(R.string.toast_error_retrieving_data));
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            if(e instanceof FileNotFoundException){
                showToast(getString(R.string.toast_stock_not_found));
            }else {
                showToast(getString(R.string.toast_no_network));
            }
        }
    }

    private void showToast(final String toastMsg){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NetworkService.this, toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
