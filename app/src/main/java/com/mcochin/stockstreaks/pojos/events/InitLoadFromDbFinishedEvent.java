package com.mcochin.stockstreaks.pojos.events;

/**
 * Created by Marco on 1/30/2016.
 */
public class InitLoadFromDbFinishedEvent extends Event {

    public InitLoadFromDbFinishedEvent(String sessionId) {
        super(sessionId);
    }
}
