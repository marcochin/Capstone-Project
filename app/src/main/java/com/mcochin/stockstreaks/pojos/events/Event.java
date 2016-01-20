package com.mcochin.stockstreaks.pojos.events;

/**
 * Created by Marco on 1/6/2016.
 */
public class Event {
    private String mSessionId;

    public Event(String  sessionId){
        mSessionId = sessionId;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }
}
