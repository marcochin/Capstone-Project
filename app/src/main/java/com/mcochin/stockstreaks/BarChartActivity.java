package com.mcochin.stockstreaks;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;

import java.util.ArrayList;

/**
 * Created by Marco on 2/2/2016.
 */
public class BarChartActivity extends AppCompatActivity {
    private final String TAG = BarChartActivity.class.getSimpleName();

    public static final String CHART_MAP_DELIMITER = ",";
    private static final int BAR_CHART_ANIMATION_DURATION = 2000;

    private Uri mDetailUri;

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

    /**
     * Initializes the bar chart.
     */
    private void initBarChart() {
        Cursor cursor = null;
        try {
            final String[] projection = new String[]{StockEntry.COLUMN_STREAK_CHART_MAP_CSV};
            final int indexStreakChartMapCsv = 0;

            mDetailUri = getIntent().getData();

            // Query db for the chart map csv string
            cursor = getContentResolver().query(mDetailUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String chartMapCsv = cursor.getString(indexStreakChartMapCsv);
                Pair<String[], int[]> chartMap = convertCsvToChartMap(chartMapCsv);
                drawChart(chartMap.first, chartMap.second);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Converts CSV string to a chart mapping we can use to plot data on our map.
     * @param chartMapCsv CSV string that will provide our chart data (x,y) mapping
     * @return The paired mapping with Pair.first being the xData and Pair.second being the yData.
     */
    private Pair<String[], int[]> convertCsvToChartMap(String chartMapCsv) {
        String[] parsedChartMapString = chartMapCsv.split(CHART_MAP_DELIMITER);

        String[] xData = new String[parsedChartMapString.length / 2];
        int[] yData = new int[parsedChartMapString.length / 2];

        // -2,3,-1,23,2,14,4,2,5,1 xData followed by yData, increment by 2
        int dataIndex = 0;
        for (int i = 0; i < parsedChartMapString.length; i += 2) {
            xData[dataIndex] = parsedChartMapString[i];
            yData[dataIndex] = Integer.parseInt(parsedChartMapString[i + 1]);
            dataIndex++;
        }
        return new Pair<>(xData, yData);
    }

    /**
     * Draws the chart using the specified xData and yData.
     * @param xData
     * @param yData
     */
    private void drawChart(String[] xData, int[] yData) {
        int[] barColors = new int[xData.length];
        // Set up bar chart settings
        final BarChart barChart = (BarChart) findViewById(R.id.bar_chart);

        // yData: Create BarEntries to be put in a List
        ArrayList<BarEntry> yDataList = new ArrayList<>();
        for (int i = 0; i < yData.length; i++) {
            BarEntry barEntry = new BarEntry(yData[i], i);
            yDataList.add(barEntry);

            if (Integer.parseInt(xData[i]) < 0) {
                barColors[i] = ContextCompat.getColor(this, R.color.pale_red);
            } else {
                barColors[i] = ContextCompat.getColor(this, R.color.pale_green);
            }

            //Format xData String to add a "d"
            xData[i] = getString(R.string.placeholder_d, xData[i]);
        }

        // Create a DataSet Wrapper for the yData list
        BarDataSet yBarDataSet = new BarDataSet(yDataList, null);
        yBarDataSet.setColors(barColors);

        // Put xData and yData together
        BarData barData = new BarData(xData, yBarDataSet);
        // Make bar value text size same as axis textSize
        barData.setValueTextSize(Utils.convertPixelsToDp(barChart.getXAxis().getTextSize()));
        // Convert the bar values from float to int
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.format("%d", (int) value);
            }
        });

        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.setGridBackgroundColor(ContextCompat.getColor(this, R.color.list_item_bg_color));
        barChart.setDescription(getString(R.string.bar_chart_description_placeholder,
                StockContract.getSymbolFromUri(mDetailUri)));

        // Need to post so when we use getWidth() it will not return 0. Anything you post to queue
        // will happen after layout pass.
        // http://stackoverflow.com/questions/3591784/getwidth-and-getheight-of-view-returns-0
        barChart.post(new Runnable() {
            @Override
            public void run() {
                barChart.setDescriptionPosition(
                        barChart.getWidth() - barChart.getViewPortHandler().offsetRight()
                                - getResources().getDimension(R.dimen.bar_chart_description_horizontal_margin),
                        barChart.getViewPortHandler().offsetTop()
                                + getResources().getDimension(R.dimen.bar_chart_description_vertical_margin));
            }
        });

        // Create custom legend
        barChart.getLegend().setCustom(
                new int[]{ContextCompat.getColor(this, R.color.pale_red),
                        ContextCompat.getColor(this, R.color.pale_green)},
                new String[]{getString(R.string.bar_chart_negative_color_label),
                        getString(R.string.bar_chart_positive_color_label)}
        );

        // Remove the default yellow no data text.
        barChart.getPaint(Chart.PAINT_INFO).setColor(
                ContextCompat.getColor(this, R.color.secondary_text));

        barChart.animateY(BAR_CHART_ANIMATION_DURATION, Easing.EasingOption.EaseOutQuart);
        barChart.setData(barData);
    }
}
