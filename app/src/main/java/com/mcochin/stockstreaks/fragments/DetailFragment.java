package com.mcochin.stockstreaks.fragments;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment that contains more details of the list items in the main list.
 */
public class DetailFragment extends Fragment{
    public static final String TAG = DetailFragment.class.getSimpleName();

    public static final String KEY_SYMBOL = "symbol";
    public static final String KEY_FULL_NAME = "fullName";
    public static final String KEY_RECENT_CLOSE = "recentCLose";
    public static final String KEY_DOLLAR_CHANGE = "dollarChange";
    public static final String KEY_PERCENT_CHANGE = "percentChange";
    public static final String KEY_STREAK = "streak";

    public static final String[] DETAIL_PROJECTION = new String[]{
            StockEntry.COLUMN_PREV_STREAK,
            StockEntry.COLUMN_PREV_STREAK_END_PRICE,
            StockEntry.COLUMN_STREAK_YEAR_HIGH,
            StockEntry.COLUMN_STREAK_YEAR_LOW
    };

    //index must match projection
    public static final int INDEX_PREV_STREAK = 0;
    public static final int INDEX_PREV_STREAK_END_PRICE = 1;
    public static final int INDEX_STREAK_YEAR_HIGH = 2;
    public static final int INDEX_STREAK_YEAR_LOW = 3;

    private TextView mTextUpdateTime;
    private TextView mTextStreakEndPricePrev;
    private TextView mTextStreakYearHigh;
    private TextView mTextStreakYearLow;
    private TextView mTextPrevStreak;
    private ImageView mImageStreakArrowPrev;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_ref, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            if(getResources().getBoolean(R.bool.is_phone)) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        Bundle args = getArguments();
        String symbol = args.getString(KEY_SYMBOL);
        String fullName = args.getString(KEY_FULL_NAME);
        float recentClose = args.getFloat(KEY_RECENT_CLOSE);
        float changeDollar = args.getFloat(KEY_DOLLAR_CHANGE);
        float changePercent = args.getFloat(KEY_PERCENT_CHANGE);
        int streak = args.getInt(KEY_STREAK);

        mTextUpdateTime = (TextView)view.findViewById(R.id.text_update_time);
        mTextPrevStreak = (TextView)view.findViewById(R.id.text_streak_prev);
        mTextStreakEndPricePrev = (TextView)view.findViewById(R.id.text_streak_end_price_prev);
        mTextStreakYearHigh = (TextView)view.findViewById(R.id.text_streak_year_high);
        mTextStreakYearLow = (TextView)view.findViewById(R.id.text_streak_year_low);
        mImageStreakArrowPrev = (ImageView)view.findViewById(R.id.image_streak_arrow_prev);

        ((TextView)view.findViewById(R.id.text_symbol)).setText(symbol);
        ((TextView)view.findViewById(R.id.text_full_name)).setText(fullName);

        ((TextView)view.findViewById(R.id.text_recent_close))
                .setText(Utility.roundTo2StringDecimals(recentClose));

        ((TextView)view.findViewById(R.id.text_streak)).setText(getString(Math.abs(streak) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, streak));

        TextView textChangeDollar = (TextView)view.findViewById(R.id.text_change_dollar);
        TextView textChangePercent = (TextView)view.findViewById(R.id.text_change_percent);
        ImageView streakArrow = (ImageView)view.findViewById(R.id.image_streak_arrow);

        // Get our dollar/percent change colors and set our stock arrow ImageView
        int color;
        if (changeDollar > 0) {
            color = ContextCompat.getColor(getContext(), R.color.stock_up_green);
            streakArrow.setBackgroundResource(R.drawable.ic_streak_up);

        } else if (changeDollar < 0) {
            color = ContextCompat.getColor(getContext(), R.color.stock_down_red);
            streakArrow.setBackgroundResource(R.drawable.ic_streak_down);

        } else {
            color = ContextCompat.getColor(getContext(), R.color.stock_neutral);
        }

        textChangeDollar.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(changeDollar)));

        textChangePercent.setText(getString(R.string.placeholder_percent,
                Utility.roundTo2StringDecimals(changePercent)));

        textChangeDollar.setTextColor(color);
        textChangePercent.setTextColor(color);

        queryDetailExtraInfo(symbol);
    }

    public void queryDetailExtraInfo(final String symbol){
        new AsyncTask<Void, Void, Void>(){
            Cursor mCursor = null;
            Date mUpdateTime;

            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = getContext().getContentResolver();
                mCursor = cr.query(
                        StockEntry.buildUri(symbol),
                        DETAIL_PROJECTION, null, null, null);

                mUpdateTime = Utility.getLastUpdateTime(cr).getTime();

                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                try{
                    if(mCursor != null && mCursor.moveToFirst()){
                        int prevStreak = mCursor.getInt(INDEX_PREV_STREAK);
                        float prevStreakEndPrice = mCursor.getFloat(INDEX_PREV_STREAK_END_PRICE);
                        int streakYearHigh = mCursor.getInt(INDEX_STREAK_YEAR_HIGH);
                        int streakYearLow = mCursor.getInt(INDEX_STREAK_YEAR_LOW);

                        mTextPrevStreak.setText(getString(Math.abs(prevStreak) == 1?
                                R.string.placeholder_day : R.string.placeholder_days, prevStreak));

                        mTextStreakEndPricePrev.setText(getString(R.string.placeholder_dollar,
                                Utility.roundTo2StringDecimals(prevStreakEndPrice)));

                        mTextStreakYearHigh.setText(getString(Math.abs(streakYearHigh) == 1 ?
                                R.string.placeholder_day : R.string.placeholder_days, streakYearHigh));

                        mTextStreakYearLow.setText(getString(Math.abs(streakYearLow) == 1 ?
                                R.string.placeholder_day : R.string.placeholder_days, streakYearLow));

                        if(prevStreak > 0){
                            mImageStreakArrowPrev.setBackgroundResource(R.drawable.ic_streak_up);
                        }else if(prevStreak < 0){
                            mImageStreakArrowPrev.setBackgroundResource(R.drawable.ic_streak_down);
                        }
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat(
                            getString(R.string.update_time_format_ref), Locale.US);
                    mTextUpdateTime.setText(getString(R.string.placeholder_update_time,
                            sdf.format(mUpdateTime)));

                }finally {
                    if (mCursor != null){
                        mCursor.close();
                    }
                }
            }
        }.execute();
    }
}
