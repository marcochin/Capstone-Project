package com.mcochin.stockstreaks.pojos;

import java.util.List;

/**
 * Created by Marco on 12/24/2015.
 */
public class LoadAFewFinishedEvent {
    private List<Stock> mStockList;
    private boolean mSuccessful;

    public LoadAFewFinishedEvent(List<Stock> stockList, boolean successful){
        mStockList = stockList;
        mSuccessful = successful;
    }

    public List<Stock> getStockList() {
        return mStockList;
    }

    public void setStockList(List<Stock> stockList) {
        mStockList = stockList;
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public void setSuccessful(boolean successful) {
        mSuccessful = successful;
    }
}
