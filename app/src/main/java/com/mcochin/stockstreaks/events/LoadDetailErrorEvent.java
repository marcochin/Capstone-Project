package com.mcochin.stockstreaks.events;

/**
 * Created by Marco on 12/24/2015.
 */
public class LoadDetailErrorEvent extends Event{
    public LoadDetailErrorEvent(String sessionId){
        super(sessionId);
    }
}
