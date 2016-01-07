package com.mcochin.stockstreaks.custom;

import android.app.Application;

import java.util.UUID;

/**
 * Custom Application class holds global state of the app.
 */
public class MyApplication extends Application {
    private static MyApplication sMyApplication;
    private static String mSessionId = "";

    /**
     * This is true if the list is updating to the latest values.
     * (Swipe to Refresh/ Refresh Menu Btn/ Widget Refresh)
     */
    private static volatile boolean mRefreshing = false;

    /**
     * This is true if the list is loading a few on the bottom
     */
    private static volatile boolean mLoadingAFew;

    public static MyApplication getInstance(){
        return sMyApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sMyApplication = this;
    }


    public static void generateSessionId(){
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

    public boolean isLoadingAFew() {
        return mLoadingAFew;
    }

    public void setLoadingAFew(boolean loadingAFew) {
        mLoadingAFew = loadingAFew;
    }
}