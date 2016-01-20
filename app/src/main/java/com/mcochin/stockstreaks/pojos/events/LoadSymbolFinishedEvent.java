package com.mcochin.stockstreaks.pojos.events;

import com.mcochin.stockstreaks.pojos.Stock;

/**
 * Created by Marco on 12/24/2015.
 */
public class LoadSymbolFinishedEvent  extends Event {
    private Stock mStock;
    private boolean mSuccessful;

    public LoadSymbolFinishedEvent(String sessionId, Stock stock, boolean successful){
        super(sessionId);
        mStock = stock;
        mSuccessful = successful;
    }

    public Stock getStock() {
        return mStock;
    }

    public void setStock(Stock stock) {
        mStock = stock;
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public void setSuccessful(boolean successful) {
        mSuccessful = successful;
    }
}
