package com.mcochin.stockstreaks.events;

/**
 * Created by Marco on 1/3/2016.
 */
public class OnWidgetRefreshEvent extends Event{
    private boolean mRefreshThroughWidget;

    public OnWidgetRefreshEvent(String sessionId, boolean refreshThroughWidget){
        super(sessionId);
        mRefreshThroughWidget = refreshThroughWidget;
    }

    public boolean isRefreshingThroughWidget() {
        return mRefreshThroughWidget;
    }

    public void setRefreshThroughWidget(boolean refreshThroughWidget) {
        mRefreshThroughWidget = refreshThroughWidget;
    }
}
