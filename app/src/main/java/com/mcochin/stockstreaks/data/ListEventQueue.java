package com.mcochin.stockstreaks.data;

import com.mcochin.stockstreaks.pojos.events.AppRefreshFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.Event;
import com.mcochin.stockstreaks.pojos.events.LoadMoreFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.WidgetRefreshDelegateEvent;

import java.util.LinkedList;
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
    private Queue<Event> mQueue;

    private ListEventQueue(){
        mQueue = new LinkedList<>();
    }

    public synchronized static ListEventQueue getInstance(){
        if(sListEventQueue == null){
            sListEventQueue = new ListEventQueue();
        }

        return sListEventQueue;
    }

    public void post(Event event){
        EventBus eventBus = EventBus.getDefault();
        Class c = getEventType(event);

        if(eventBus.hasSubscriberForEvent(c) && mQueue.isEmpty()){
            eventBus.postSticky(event);
        }else{
            mQueue.offer(event);
        }
    }

    public void postAllFromQueue(){
        EventBus eventBus = EventBus.getDefault();

        while(!mQueue.isEmpty()){
            Class event = getEventType(mQueue.peek());

            if(eventBus.hasSubscriberForEvent(event)){
                eventBus.postSticky(mQueue.poll());
            }else{
                break;
            }
        }
    }
    /**
     * Determines what class the event is.
     * @param event The event to figure out the type for.
     * @return Class obj that represents the event class.
     */

    private Class getEventType(Event event) {
        if (event instanceof LoadMoreFinishedEvent) {
            return LoadMoreFinishedEvent.class;

        } else if (event instanceof LoadSymbolFinishedEvent) {
            return LoadSymbolFinishedEvent.class;

        } else if (event instanceof WidgetRefreshDelegateEvent) {
            return WidgetRefreshDelegateEvent.class;

        } else if (event instanceof AppRefreshFinishedEvent) {
            return AppRefreshFinishedEvent.class;
        }
        return null;
    }

    public Object peek(){
        return mQueue.peek();
    }

    public void clearQueue(){
        mQueue.clear();
    }

    public boolean isEmpty(){
        return mQueue.isEmpty();
    }
}


