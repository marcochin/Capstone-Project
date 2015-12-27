package com.mcochin.stockstreaks.pojos;

/**
 * Pojo for a stock
 */
public class Stock {
    private int mId;
    private String mSymbol;
    private String mFullName;
    private float mRecentClose;
    private int mStreak;
    private float mChangeDollar;
    private float mChangePercent;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public void setSymbol(String mSymbol) {
        this.mSymbol = mSymbol;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String mFullName) {
        this.mFullName = mFullName;
    }

    public float getRecentClose() {
        return mRecentClose;
    }

    public void setRecentClose(float mRecentCLose) {
        this.mRecentClose = mRecentCLose;
    }

    public int getStreak() {
        return mStreak;
    }

    public void setStreak(int streak) {
        this.mStreak = streak;
    }

    public float getChangeDollar() {
        return mChangeDollar;
    }

    public void setChangeDollar(float mDollarChange) {
        this.mChangeDollar = mDollarChange;
    }

    public float getChangePercent() {
        return mChangePercent;
    }

    public void setChangePercent(float percentChange) {
        this.mChangePercent = percentChange;
    }
}
