package com.mcochin.stockstreaks.events;

/**
 * Created by Marco on 1/3/2016.
 */
public class WidgetRefreshEvent {
    private boolean mRefreshThroughWidget;

    public WidgetRefreshEvent(boolean refreshThroughWidget){
        mRefreshThroughWidget = refreshThroughWidget;
    }

    public boolean isRefreshThroughWidget() {
        return mRefreshThroughWidget;
    }

    public void setRefreshThroughWidget(boolean refreshThroughWidget) {
        mRefreshThroughWidget = refreshThroughWidget;
    }
}
