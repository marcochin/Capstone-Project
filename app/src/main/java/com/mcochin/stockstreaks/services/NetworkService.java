package com.mcochin.stockstreaks.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.mcochin.stockstreaks.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Service that goes online to retrieve our information!
 */
public class NetworkService extends IntentService {
    private static final String TAG = NetworkService.class.getSimpleName();
    public static String KEY_SEARCH_QUERY ="searchQuery";
//    private static String SEARCH_REQUEST = NetworkService.class.getSimpleName();
//    private static String LIST_REFRESH_REQUEST = NetworkService.class.getSimpleName();
    private static final int MONTH = 31;
    private static final int YEAR = 365;

    public NetworkService(){
        super("NetworkService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String query = intent.getStringExtra(KEY_SEARCH_QUERY);

        Calendar toCalendar = Calendar.getInstance();
        Calendar fromCalendar = Calendar.getInstance();
        //We want historical data for the past month
        fromCalendar.add(Calendar.DAY_OF_MONTH, -5);

        try {
            // TODO add more reliable check for internet
            Stock stock = YahooFinance.get(query, fromCalendar, toCalendar, Interval.DAILY);
            if(stock == null){
                showToast(getString(R.string.toast_error_retrieving_data));

            } else{
                Log.d(TAG, "Symbol: " + stock.getSymbol()
                        + " Full name: " + stock.getName()
                        + " Prev. Close: " + stock.getQuote().getPreviousClose()
                        + " Current Stock Price: " + stock.getQuote().getPrice()
                        + " Change $: " + stock.getQuote().getChange()
                        + " Change %: " + stock.getQuote().getChangeInPercent());


                List<HistoricalQuote> history = stock.getHistory();
                Log.d(TAG, stock.getHistory() + "");
                for(HistoricalQuote h : history){
                    Log.d(TAG, "Date: " + h.getDate().get(Calendar.MONTH)
                            + h.getDate().get(Calendar.DAY_OF_MONTH)
                            + h.getDate().get(Calendar.YEAR)
                            + " Close: " + h.getClose()
                            + " Adjusted close: " + h.getAdjClose());
                }
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

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NetworkService.this, toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void debugToast(final String toastMsg){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NetworkService.this, toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
