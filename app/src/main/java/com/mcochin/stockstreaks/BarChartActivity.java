package com.mcochin.stockstreaks;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.pojos.StreakFrequency;

import java.util.List;

/**
 * Created by Marco on 2/2/2016.
 */
public class BarChartActivity extends AppCompatActivity {
    private final String TAG = BarChartActivity.class.getSimpleName();
    private final String KEY_CHART_MAP = "chartMap";
    private final String KEY_DETAIL_URI = "detailUri";

    public static final String CHART_MAP_DELIMITER = ",";

    private List<StreakFrequency> mChartMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_chart);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initBarChart();
    }

    private void initBarChart(){
        Cursor cursor = null;
        try{
            final String[] projection = new String[]{StockEntry.COLUMN_STREAK_CHART_MAP};
            final int indexStreakChartMap = 0;

            Uri detailUri  = getIntent().getData();
            cursor = getContentResolver().query(detailUri, projection, null, null, null);

            if(cursor != null && cursor.moveToFirst()){
                String chartMapString = cursor.getString(indexStreakChartMap);
                mChartMap = convertStringToChartMap(chartMapString);
                drawChart(mChartMap);
            }

        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    private List<StreakFrequency> convertStringToChartMap(String chartMapString){
        String[] parsedChartMapString = chartMapString.split(CHART_MAP_DELIMITER);
        for(String s: parsedChartMapString){
            Log.d(TAG, s);
        }
        return null;
    }

    private void drawChart(List<StreakFrequency> chartMap){

    }
}
