package com.mcochin.stockstreakz.pojos;

/**
 * Pojo for a stock
 */
public class Stock {
    private int mId;
    private String mSymbol;
    private String mFullName;
    private float mPrevClose;
    private int mStreak;
    private float mDollarChange;
    private float mPercentChange;
    private int mPrevStreak;
    private float mPrevStreakPrice;
    private int mYearStreakHigh;
    private int mYearStreakLow;
    private boolean mCanDrag;

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

    public String getmFullName() {
        return mFullName;
    }

    public void setmFullName(String mFullName) {
        this.mFullName = mFullName;
    }

    public float getmPrevClose() {
        return mPrevClose;
    }

    public void setmPrevClose(float mPrevClose) {
        this.mPrevClose = mPrevClose;
    }

    public int getStreak() {
        return mStreak;
    }

    public void setStreak(int streak) {
        this.mStreak = streak;
    }

    public float getmDollarChange() {
        return mDollarChange;
    }

    public void setmDollarChange(float mDollarChange) {
        this.mDollarChange = mDollarChange;
    }

    public float getPercentChange() {
        return mPercentChange;
    }

    public void setPercentChange(float percentChange) {
        this.mPercentChange = percentChange;
    }

    public int getPrevStreak() {
        return mPrevStreak;
    }

    public void setPrevStreak(int prevStreak) {
        this.mPrevStreak = prevStreak;
    }

    public float getPrevStreakPrice() {
        return mPrevStreakPrice;
    }

    public void setPrevStreakPrice(float prevStreakPrice) {
        this.mPrevStreakPrice = prevStreakPrice;
    }

    public int getYearStreakHigh() {
        return mYearStreakHigh;
    }

    public void setYearStreakHigh(int yearStreakHigh) {
        this.mYearStreakHigh = yearStreakHigh;
    }

    public int getYearStreakLow() {
        return mYearStreakLow;
    }

    public void setYearStreakLow(int yearStreakLow) {
        this.mYearStreakLow = yearStreakLow;
    }

    public boolean isCanDrag() {
        return mCanDrag;
    }

    public void setCanDrag(boolean canDrag) {
        this.mCanDrag = mCanDrag;
    }
}
