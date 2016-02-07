package com.mcochin.stockstreaks.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Toast;

import com.mcochin.stockstreaks.BarChartActivity;
import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.pojos.events.LoadDetailFinishedEvent;
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
    public static final String KEY_IS_DETAIL_REQUEST_LOADING= "isDetailRequestLoading";
    public static final String KEY_DETAIL_URI = "detailUri";


    public static final String[] DETAIL_PROJECTION = new String[]{
            StockEntry.COLUMN_SYMBOL,
            StockEntry.COLUMN_FULL_NAME,
            StockEntry.COLUMN_RECENT_CLOSE,
            StockEntry.COLUMN_CHANGE_DOLLAR,
            StockEntry.COLUMN_CHANGE_PERCENT,
            StockEntry.COLUMN_STREAK,
            StockEntry.COLUMN_PREV_STREAK,
            StockEntry.COLUMN_PREV_STREAK_END_PRICE,
            StockEntry.COLUMN_STREAK_YEAR_HIGH,
            StockEntry.COLUMN_STREAK_YEAR_LOW
    };

    //index must match projection
    public static final int INDEX_SYMBOL = 0;
    public static final int INDEX_FULL_NAME = 1;
    public static final int INDEX_RECENT_CLOSE = 2;
    public static final int INDEX_CHANGE_DOLLAR = 3;
    public static final int INDEX_CHANGE_PERCENT = 4;
    public static final int INDEX_STREAK = 5;
    public static final int INDEX_PREV_STREAK = 6;
    public static final int INDEX_PREV_STREAK_END_PRICE = 7;
    public static final int INDEX_STREAK_YEAR_HIGH = 8;
    public static final int INDEX_STREAK_YEAR_LOW = 9;

    private View mMainInfo;
    private View mExtrasInfo;
    private View mProgressWheel;
    private View mRetryButton;

    private TextView mTextUpdateTime;
    private TextView mTextPrevStreak;

    private Uri mDetailUri;
    private boolean mReplyButtonVisible;
    private boolean mIsDetailRequestLoading;

    private Toast mBarChartButtonToast;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Need to postpone transition until data is loaded because if data is loading while
            // transition is happening setting visibility of some items to VISIBLE will make the
            // items appear instantly instead of being transitioned.
            getActivity().postponeEnterTransition();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_ref, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        mDetailUri = args.getParcelable(KEY_DETAIL_URI);

        if(savedInstanceState != null){
            mReplyButtonVisible = savedInstanceState.getBoolean(KEY_REPLY_BUTTON_VISIBLE);
            mIsDetailRequestLoading = savedInstanceState.getBoolean(KEY_IS_DETAIL_REQUEST_LOADING);
        }

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if(getResources().getBoolean(R.bool.is_phone)) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
            ActionBar actionBar = activity.getSupportActionBar();

            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            setHasOptionsMenu(true);

        }else{
            toolbar.setVisibility(View.GONE);
        }

        mExtrasInfo = view.findViewById(R.id.detail_extras_info);
        mMainInfo = view.findViewById(R.id.detail_main_info);
        mTextUpdateTime = (TextView)view.findViewById(R.id.text_update_time);
        mTextPrevStreak = (TextView)mExtrasInfo.findViewById(R.id.text_streak_prev);

        mProgressWheel = view.findViewById(R.id.progress_wheel);
        mRetryButton = view.findViewById(R.id.button_retry);

        mRetryButton.setOnClickListener(this);
        view.findViewById((R.id.button_bar_chart)).setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus eventBus = EventBus.getDefault();
        eventBus.registerSticky(this);

        if(mTextPrevStreak.getText().toString().isEmpty()) {
            fetchDetailsData();
        }
    }

    /**
     * Fetches the detail data from the db of the selected stock using a cursor loader.
     */
    private void fetchDetailsData(){
        showProgressWheel();

        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        loaderManager.restartLoader(ID_LOADER_DETAILS, null, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_retry:
                mReplyButtonVisible = false;
                startDetailService();
                break;

            case R.id.button_bar_chart:
                if(mExtrasInfo.getVisibility() == View.VISIBLE) {
                    Intent barChartIntent = new Intent(getActivity(), BarChartActivity.class);
                    barChartIntent.setData(mDetailUri);
                    startActivity(barChartIntent);

                }else if(mBarChartButtonToast == null){
                    mBarChartButtonToast = Toast.makeText(getActivity(),
                            R.string.toast_chart_data_not_yet_available,
                            Toast.LENGTH_SHORT);
                    mBarChartButtonToast.show();

                }else if(!mBarChartButtonToast.getView().isShown()){
                    //TODO try setText thing
                    mBarChartButtonToast.show();
                }
                break;
        }
    }

    /**
     * Starts the a {@link DetailService} to perform a network request to retrieve the symbol's
     * history.
     */
    private void startDetailService(){
        mIsDetailRequestLoading = true;
        showProgressWheel();

        Intent serviceIntent = new Intent(getActivity(), DetailService.class);
        serviceIntent.putExtra(DetailService.KEY_DETAIL_SYMBOL,
                StockContract.getSymbolFromUri(mDetailUri));
        getActivity().startService(serviceIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                mDetailUri,
                DETAIL_PROJECTION,
                null,
                null,
                null);
    }

    // This function is also guaranteed to be called prior to the release of the last data that was
    // supplied for this Loader." During onResume it is perfectly reasonable that the loader
    // releases its data and reloads during onResume. Yes, if you are seeing a behavior where the
    // loader may callback and you don't want that callback, then destroy the loader.
    // http://stackoverflow.com/questions/21031692/why-is-onloadfinished-called-again-after-fragment-resumed
    // I destroy the Loader when I finished getting the extras section, so it doesn't happen.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null && data.moveToFirst()){
            int prevStreak = data.getInt(INDEX_PREV_STREAK);

            // Set update time
            Date lastUpdate = Utility.getLastUpdateTime(
                    getActivity().getContentResolver()).getTime();
            SimpleDateFormat sdf = new SimpleDateFormat(
                    getString(R.string.update_time_format), Locale.US);
            String lastUpdateString = getString(R.string.placeholder_update_time,
                    sdf.format(lastUpdate));

            // Main Section
            // Add check here so when the service returns from calculating the prev streak info
            // it wont have to load main section again.
            if(!mTextUpdateTime.getText().toString().equals(lastUpdateString)) {
                initMainSection(data, lastUpdateString);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().startPostponedEnterTransition();
                }
            }
            // Extras Section
            if(prevStreak != 0) {
                initExtrasSection(data, prevStreak);

            }else if(mReplyButtonVisible){
                showRetryButton();

            }else if(!mIsDetailRequestLoading){
                startDetailService();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void onEventMainThread(LoadDetailFinishedEvent event){
        // Make sure we don't process the event of another stock symbol. This can happen is we
        // switch to a different DetailFragment while the prev one is still loading.
        if(event.getSymbol().equals(getSymbol())) {
            mIsDetailRequestLoading = false;

            if(!event.isSuccessful()) {
                showRetryButton();
            }else{
                showExtrasInfo();
            }
        }
        EventBus.getDefault().removeStickyEvent(LoadDetailFinishedEvent.class);
    }

    private void initMainSection(Cursor data, String lastUpdateString){
        TextView textSymbol = (TextView)mMainInfo.findViewById(R.id.text_symbol);
        TextView textFullName = (TextView)mMainInfo.findViewById(R.id.text_full_name);
        TextView textRecentClose = (TextView)mMainInfo.findViewById(R.id.text_recent_close);
        TextView textChangeDollar = (TextView)mMainInfo.findViewById(R.id.text_change_dollar);
        TextView textChangePercent = (TextView)mMainInfo.findViewById(R.id.text_change_percent);
        TextView textStreak = (TextView)mMainInfo.findViewById(R.id.text_streak);
        TextView textChangePercentNegSign = (TextView)mMainInfo.findViewById(R.id.text_change_percent_neg_sign);
        ImageView imageStreakArrow = (ImageView)mMainInfo.findViewById(R.id.image_streak_arrow);
        View textStreakNegSign = mMainInfo.findViewById(R.id.text_streak_neg_sign);

        String symbol = data.getString(INDEX_SYMBOL);
        String fullName = data.getString(INDEX_FULL_NAME);
        float recentClose = data.getFloat(INDEX_RECENT_CLOSE);
        float changeDollar = data.getFloat(INDEX_CHANGE_DOLLAR);
        float changePercent = data.getFloat(INDEX_CHANGE_PERCENT);
        int streak = data.getInt(INDEX_STREAK);

        mTextUpdateTime.setText(lastUpdateString);

        textSymbol.setText(symbol);
        textFullName.setText(fullName);

        textRecentClose.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(recentClose)));

        if(streak < 0){
            textStreakNegSign.setVisibility(View.VISIBLE);
        }
        textStreak.setText(getString(Math.abs(streak) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(streak)));

        // Get our dollar/percent change colors and set our stock arrow ImageView
        // Determine the color and the arrow image of the changes
        Pair<Integer, Integer> changeColorAndDrawableIds =
                Utility.getChangeColorAndArrowDrawableIds(streak);

        int color = ContextCompat.getColor(getActivity(), changeColorAndDrawableIds.first);
        imageStreakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

        textChangeDollar.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(changeDollar)));

        if(changePercent < 0){
            textChangePercentNegSign.setVisibility(View.VISIBLE);
            textChangePercentNegSign.setTextColor(color);
        }
        textChangePercent.setText(getString(R.string.placeholder_percent,
                Utility.roundTo2StringDecimals(Math.abs(changePercent))));

        textChangeDollar.setTextColor(color);
        textChangePercent.setTextColor(color);
    }

    private void initExtrasSection(Cursor data, int prevStreak){
        TextView mTextPrevStreakEndPrice = (TextView)mExtrasInfo.findViewById(R.id.text_prev_streak_end_price);
        TextView mTextStreakYearHigh = (TextView)mExtrasInfo.findViewById(R.id.text_streak_year_high);
        TextView mTextStreakYearLow = (TextView)mExtrasInfo.findViewById(R.id.text_streak_year_low);
        ImageView mImagePrevStreakArrow = (ImageView)mExtrasInfo.findViewById(R.id.image_prev_streak_arrow);
        View textPrevStreakNegSign = mExtrasInfo.findViewById(R.id.text_streak_prev_neg_sign);

        float prevStreakEndPrice = data.getFloat(INDEX_PREV_STREAK_END_PRICE);
        int streakYearHigh = data.getInt(INDEX_STREAK_YEAR_HIGH);
        int streakYearLow = data.getInt(INDEX_STREAK_YEAR_LOW);

        mIsDetailRequestLoading = false;

        if(prevStreak < 0){
            textPrevStreakNegSign.setVisibility(View.VISIBLE);
        }
        mTextPrevStreak.setText(getString(Math.abs(prevStreak) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(prevStreak)));

        Pair<Integer, Integer> changeColorAndDrawableIds =
                Utility.getChangeColorAndArrowDrawableIds(prevStreak);
        mImagePrevStreakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

        mTextPrevStreakEndPrice.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(prevStreakEndPrice)));

        mTextStreakYearHigh.setText(getString(Math.abs(streakYearHigh) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, streakYearHigh));

        mTextStreakYearLow.setText(getString(Math.abs(streakYearLow) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(streakYearLow)));

        showExtrasInfo();
        getActivity().getSupportLoaderManager().destroyLoader(ID_LOADER_DETAILS);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REPLY_BUTTON_VISIBLE, mReplyButtonVisible);
        outState.putBoolean(KEY_IS_DETAIL_REQUEST_LOADING, mIsDetailRequestLoading);
        super.onSaveInstanceState(outState);
    }

    private void showProgressWheel(){
        mProgressWheel.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        mExtrasInfo.setVisibility(View.INVISIBLE);
    }

    private void showRetryButton(){
        mReplyButtonVisible = true;
        mProgressWheel.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);
        mExtrasInfo.setVisibility(View.INVISIBLE);
    }

    private void showExtrasInfo(){
        mProgressWheel.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        mExtrasInfo.setVisibility(View.VISIBLE);
    }

    public String getSymbol(){
        return StockContract.getSymbolFromUri(mDetailUri);
    }
}
