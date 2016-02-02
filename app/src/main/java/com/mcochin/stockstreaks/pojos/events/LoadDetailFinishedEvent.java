package com.mcochin.stockstreaks.pojos.events;

/**
 * Created by Marco on 12/24/2015.
 */
public class LoadDetailFinishedEvent extends Event {
    private String mSymbol;
    private boolean mSuccessful;

    public LoadDetailFinishedEvent(String sessionId, String symbol, boolean successful){
        super(sessionId);
        mSymbol = symbol;
        mSuccessful = successful;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public void setSymbol(String symbol) {
        mSymbol = symbol;
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public void setSuccessful(boolean successful) {
        mSuccessful = successful;
    }
}
