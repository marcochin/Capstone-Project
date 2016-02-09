package com.mcochin.stockstreaks.custom;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.mcochin.stockstreaks.R;

import java.util.UUID;

/**
 * Custom Application class holds global state of the app.
 */
public class MyApplication extends Application {
    private static MyApplication sMyApplication;

    // Even though we are using TagManager to send Analytics Hits, we still need Tracker
    // to monitor uncaught exceptions
    private Tracker mTracker;
    private TagManager mTagManager;
    private ContainerHolder mContainerHolder;

    private static String mSessionId = "";

    /**
     * This is true if the list is updating to the latest values.
     * (Swipe to Refresh/ Refresh Menu Btn/ Widget Refresh)
     */
    private static volatile boolean mRefreshing = false;

    public static MyApplication getInstance(){
        return sMyApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sMyApplication = this;
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    public synchronized Tracker getAnalyticsTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.analytics_tracker);
        }
        return mTracker;
    }

    public synchronized TagManager getTagManager(){
        if (mTagManager == null) {
            mTagManager = TagManager.getInstance(this);
        }
        return mTagManager;
    }

    public ContainerHolder getContainerHolder() {
        return mContainerHolder;
    }

    public void setContainerHolder(ContainerHolder containerHolder) {
        mContainerHolder = containerHolder;
    }

    public static void startNewSession(){
        mSessionId = UUID.randomUUID().toString();
    }

    public static boolean validateSessionId(String sessionId){
        if(sessionId == null || sessionId.isEmpty()){
            return false;
        }

        return mSessionId.equals(sessionId);
    }

    public String getSessionId(){
        return mSessionId;
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    public void setRefreshing(boolean refreshing) {
        mRefreshing = refreshing;
    }
}