package com.mcochin.stockstreaks.pojos.events;

/**
 * Created by Marco on 12/24/2015.
 */
public class LoadDetailErrorEvent extends Event {
    private String mSymbol;
    public LoadDetailErrorEvent(String sessionId, String symbol){
        super(sessionId);
        mSymbol = symbol;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public void setSymbol(String symbol) {
        mSymbol = symbol;
    }
}
