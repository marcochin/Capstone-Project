package com.mcochin.stockstreaks.utils;

import com.mcochin.stockstreaks.pojos.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadSymbolFinishedEvent;

import java.util.PriorityQueue;

import de.greenrobot.event.EventBus;

/**
 * This is a queue for our EventBus events. If an event is sent with no subscriber, it will be put
 * into the queue.This can occur during orientation change causes a subscriber to unregister right
 * as the event is posted. After orientation change it should process events from this queue.
 */
public class ListEventQueue {

    private static ListEventQueue sListEventQueue;
    private PriorityQueue<Object> mQueue;

    private ListEventQueue(){
        mQueue = new PriorityQueue<>();
    }

    public synchronized static ListEventQueue getInstance(){
        if(sListEventQueue == null){
            sListEventQueue = new ListEventQueue();
        }

        return sListEventQueue;
    }

    public void post(Object event){
        EventBus eventBus = EventBus.getDefault();
        Class c = getClassFromEvent(event);

        if(eventBus.hasSubscriberForEvent(c) && mQueue.isEmpty()){
            eventBus.post(event);
        }else{
            mQueue.offer(event);
        }
    }

    public void postPop(){
        EventBus eventBus = EventBus.getDefault();
        Class c = getClassFromEvent(mQueue.peek());

        if(eventBus.hasSubscriberForEvent(c)){
            eventBus.post(mQueue.poll());
        }
    }

    private Class getClassFromEvent(Object event){
        if(event instanceof LoadAFewFinishedEvent){
            return LoadAFewFinishedEvent.class;
        }else if (event instanceof LoadSymbolFinishedEvent){
            return LoadSymbolFinishedEvent.class;
        }

        return null;
    }

    public boolean isEmpty(){
        return mQueue.isEmpty();
    }
}
