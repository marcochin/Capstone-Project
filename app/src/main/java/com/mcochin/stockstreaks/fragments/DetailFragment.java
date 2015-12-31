package com.mcochin.stockstreaks.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
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
import com.mcochin.stockstreaks.pojos.LoadDetailErrorEvent;
import com.mcochin.stockstreaks.services.DetailService;
import com.mcochin.stockstreaks.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.greenrobot.event.EventBus;

/**
 * Fragment that contains more details of the list items in the main list.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    public static final String TAG = DetailFragment.class.getSimpleName();

    public static final int ID_LOADER_DETAILS = 2;
    public static final String KEY_REPLY_BUTTON_VISIBLE = "replyButtonVisible";

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

    private View mProgressWheel;
    private View mRetryButton;
    private View mExtrasSection;
    private TextView mTextStreakEndPricePrev;
    private TextView mTextStreakYearHigh;
    private TextView mTextStreakYearLow;
    private TextView mTextPrevStreak;
    private ImageView mImageStreakArrowPrev;

    private String mSymbol;
    private boolean mReplyButtonVisible;

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
            if(getResources().getBoolean(R.bool.is_phone)) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }else{
                toolbar.setVisibility(View.GONE);
            }
        }
        Bundle args = getArguments();
        mSymbol = args.getString(KEY_SYMBOL);

        if(savedInstanceState != null){
            mReplyButtonVisible = savedInstanceState.getBoolean(KEY_REPLY_BUTTON_VISIBLE);
        }

        initializeInitialViews(view, args);
        initializeDetailExtrasSection();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REPLY_BUTTON_VISIBLE, mRetryButton.getVisibility() == View.VISIBLE);
        super.onSaveInstanceState(outState);
    }

    private void initializeInitialViews(View view, Bundle args){
        String symbol = args.getString(KEY_SYMBOL);
        String fullName = args.getString(KEY_FULL_NAME);
        float recentClose = args.getFloat(KEY_RECENT_CLOSE);
        float changeDollar = args.getFloat(KEY_DOLLAR_CHANGE);
        float changePercent = args.getFloat(KEY_PERCENT_CHANGE);
        int streak = args.getInt(KEY_STREAK);

        mProgressWheel = view.findViewById(R.id.progress_wheel);
        mRetryButton = view.findViewById(R.id.retry_button);
        mExtrasSection = view.findViewById(R.id.detail_extras_section);
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
        TextView updateTime = (TextView)view.findViewById(R.id.text_update_time);

        mRetryButton.setOnClickListener(this);

        // Get our dollar/percent change colors and set our stock arrow ImageView
        //Determine the color and the arrow image of the changes
        Pair<Integer, Integer> changeColorAndDrawableIds =
                Utility.getChangeColorAndArrowDrawableIds(changeDollar);
        int color = ContextCompat.getColor(getContext(), changeColorAndDrawableIds.first);
        streakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

        textChangeDollar.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(changeDollar)));
        textChangePercent.setText(getString(R.string.placeholder_percent,
                Utility.roundTo2StringDecimals(changePercent)));
        textChangeDollar.setTextColor(color);
        textChangePercent.setTextColor(color);

        // Set update time
        Date lastUpdate = Utility.getLastUpdateTime(getContext().getContentResolver()).getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(
                getString(R.string.detail_update_time_format_ref), Locale.US);

        updateTime.setText(getString(R.string.placeholder_update_time,
                sdf.format(lastUpdate)));
    }

    private void initializeDetailExtrasSection(){
        mProgressWheel.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        LoaderManager loaderManager = ((AppCompatActivity)getContext()).getSupportLoaderManager();
        loaderManager.initLoader(ID_LOADER_DETAILS, null, this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.retry_button:
                initializeDetailExtrasSection();
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
                StockEntry.buildUri(mSymbol),
                DETAIL_PROJECTION,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null && data.moveToFirst()){

            int prevStreak = data.getInt(INDEX_PREV_STREAK);
            float prevStreakEndPrice = data.getFloat(INDEX_PREV_STREAK_END_PRICE);
            int streakYearHigh = data.getInt(INDEX_STREAK_YEAR_HIGH);
            int streakYearLow = data.getInt(INDEX_STREAK_YEAR_LOW);

            if(prevStreak != 0) {
                mTextPrevStreak.setText(getString(Math.abs(prevStreak) == 1 ?
                        R.string.placeholder_day : R.string.placeholder_days, prevStreak));

                mTextStreakEndPricePrev.setText(getString(R.string.placeholder_dollar,
                        Utility.roundTo2StringDecimals(prevStreakEndPrice)));

                mTextStreakYearHigh.setText(getString(Math.abs(streakYearHigh) == 1 ?
                        R.string.placeholder_day : R.string.placeholder_days, streakYearHigh));

                mTextStreakYearLow.setText(getString(Math.abs(streakYearLow) == 1 ?
                        R.string.placeholder_day : R.string.placeholder_days, streakYearLow));

                if (prevStreak > 0) {
                    mImageStreakArrowPrev.setBackgroundResource(R.drawable.ic_streak_up);
                } else if (prevStreak < 0) {
                    mImageStreakArrowPrev.setBackgroundResource(R.drawable.ic_streak_down);
                }

                mProgressWheel.setVisibility(View.INVISIBLE);
                mExtrasSection.setVisibility(View.VISIBLE);

                //TODO dont destroy loader because of tablet. When a user refreshes list
                // the old fragment might still be in there
                ((AppCompatActivity)getContext()).getSupportLoaderManager()
                        .destroyLoader(ID_LOADER_DETAILS);

            }else if(mReplyButtonVisible){
                mProgressWheel.setVisibility(View.INVISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);

            } else{
                Intent serviceIntent = new Intent(getContext(), DetailService.class);
                serviceIntent.putExtra(DetailService.KEY_DETAIL_SYMBOL, mSymbol);
                getContext().startService(serviceIntent);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void onEventMainThread(LoadDetailErrorEvent event){
        mProgressWheel.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);
    }
}
