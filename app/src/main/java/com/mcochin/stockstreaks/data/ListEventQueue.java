package com.mcochin.stockstreaks.data;

import android.util.Log;

import com.mcochin.stockstreaks.pojos.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadFromDbFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadSymbolFinishedEvent;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import de.greenrobot.event.EventBus;

/**
 * This is a queue for our EventBus events. If an event is sent with no subscriber OR if queue not
 * empty, it will be put into the queue. This can occur during orientation change which can cause
 * a subscriber to unregister right as the event is posted. After orientation change it should
 * process events from this queue.
 */
public class ListEventQueue {
    private static final String TAG = ListEventQueue.class.getSimpleName();
    private static ListEventQueue sListEventQueue;
    private Queue<Object> mQueue;

    private ListEventQueue(){
        mQueue = new LinkedList<>();
    }

    public synchronized static ListEventQueue getInstance(){
        if(sListEventQueue == null){
            sListEventQueue = new ListEventQueue();
        }

        return sListEventQueue;
    }

    public void post(Object event){
        EventBus eventBus = EventBus.getDefault();
        Class c = getEventType(event);

        if(eventBus.hasSubscriberForEvent(c) && mQueue.isEmpty()){
            eventBus.post(event);
        }else{
            mQueue.offer(event);
        }
    }

    public void postAllFromQueue(){
        EventBus eventBus = EventBus.getDefault();

        while(!mQueue.isEmpty()){
            Class event = getEventType(mQueue.peek());

            if(eventBus.hasSubscriberForEvent(event)){
                eventBus.post(mQueue.poll());
                Log.d(TAG, "posted");
            }else{
                Log.d(TAG, "did not post");
                break;
            }
        }
    }

    public void clearQueue(){
        mQueue.clear();
    }

    private Class getEventType(Object event){
        if(event instanceof LoadAFewFinishedEvent){
            return LoadAFewFinishedEvent.class;
        }else if (event instanceof LoadSymbolFinishedEvent){
            return LoadSymbolFinishedEvent.class;
        }else if (event instanceof LoadFromDbFinishedEvent){
            return LoadFromDbFinishedEvent.class;
        }

        return null;
    }

    public boolean isEmpty(){
        return mQueue.isEmpty();
    }
}
