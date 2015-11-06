package com.mcochin.stockstreakz.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;
import com.mcochin.stockstreakz.R;
import com.mcochin.stockstreakz.data.ListManipulator;

/**
 * This is the adapter for <code>MainFragment</code>
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainViewHolder>
        implements DraggableItemAdapter<MainAdapter.MainViewHolder>,
        SwipeableItemAdapter<MainAdapter.MainViewHolder> {

    public static final String TAG = MainAdapter.class.getSimpleName();
    private ListManipulator mListManipulator;
    private EventListener mEventListener;

    /**
     * This boolean is used to prevent drag from initiating while a swipe is occurring. Without this
     * flag, if you hold the swipe for too long it will trigger the onLongClick drag to initiate.
     */
    private boolean mSwiping;

    /**
     * For some reason onCheckCanStartDrag() gets called after every FULL swipe is performed.
     * This can involuntarily initiate a drag if you swipe the screen immediately after a full swipe.
     * If onCheckCanStartDrag() is called from this special case, we need to prevent drag with this
     * flag.
     */
    private boolean mFullSwipePerformed;

    // Our ViewHolder class
    public class MainViewHolder extends AbstractDraggableSwipeableItemViewHolder
            implements View.OnClickListener {
        TextView mSymbol;
        ViewGroup mContainer;

        public MainViewHolder(View itemView) {
            super(itemView);
            mContainer = (ViewGroup) itemView.findViewById(R.id.swipe_container);
            mSymbol = (TextView) itemView.findViewById(R.id.text_symbol);
            itemView.setOnClickListener(this);
        }

        public CharSequence getSymbol(){
            return mSymbol.getText();
        }

        @Override
        public View getSwipeableContainerView() {
            return mContainer;
        }

        @Override
        public void onClick(View v) {
            mEventListener.onItemClick(this);
        }
    }

    // Interfaces
    public interface EventListener {
        void onItemClick(MainViewHolder holder);
        void onItemRemoved(MainViewHolder holder);
    }

    // NOTE: Make accessible with shorter name
    private interface Draggable extends DraggableItemConstants {
    }
    private interface Swipeable extends SwipeableItemConstants {
    }

    // MainAdapter methods start here
    public MainAdapter (EventListener eventListener, ListManipulator listManipulator){
        mEventListener = eventListener;

        listManipulator.setData(null); //TODO remove this, only for debugging
        mListManipulator = listManipulator;

        // DraggableItemAdapter and SwipeableItemAdapter require stable IDs, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.list_item, parent, false);
        return new MainViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MainViewHolder holder, int position) {
        //Log.d(TAG, "onBind");
        holder.mSymbol.setText(mListManipulator.getItem(position).getSymbol());

        // set background resource (target view ID: container)
        final int dragState = holder.getDragStateFlags();

        if (((dragState & Draggable.STATE_FLAG_IS_UPDATED) != 0)){
            int bgResId;

            // ACTIVE flags is the one being acted upon
            if ((dragState & Draggable.STATE_FLAG_IS_ACTIVE) != 0) {
                bgResId = R.drawable.bg_item_dragging_active_state;
            } else {
                bgResId = R.drawable.list_item_selector;
            }

            holder.mContainer.setBackgroundResource(bgResId);
        }

        // Set swiping properties
        // This sets the horizontal offset of the items
        holder.setSwipeItemHorizontalSlideAmount(0);
    }

    @Override
    public int getItemCount() {
        return mListManipulator.getCount();
    }

    @Override
    public long getItemId(int position) {
        return mListManipulator.getItem(position).getId();
    }

    @Override // SwipeableItemAdapter
    public SwipeResultAction onSwipeItem(MainViewHolder holder, int position, int result) {
        //Log.d(TAG, "onSwipeItem(position = " + position + ", result = " + result + ")");

        switch (result) {
            // swipe right
            case Swipeable.RESULT_SWIPED_RIGHT:
                //Log.d(TAG, "Swiped right");
            // swipe left
            case Swipeable.RESULT_SWIPED_LEFT:
                //Log.d(TAG, "Swiped left");
                return new SwipeResultAction(this, holder, position);
            // other --- do nothing
            case Swipeable.RESULT_CANCELED:
            default:
                return null;
        }
    }

    @Override // SwipeableItemAdapter
    public int onGetSwipeReactionType(MainViewHolder holder, int position, int x, int y) {
        //Log.d(TAG, "onGetSwipeReactionType");

        mSwiping = false;
        return Swipeable.REACTION_CAN_SWIPE_BOTH_H;
    }

    @Override // SwipeableItemAdapter
    public void onSetSwipeBackground(MainViewHolder holder, int position, int type) {
        //Log.d(TAG, "onSetSwipeBackground");

        mSwiping = true;
        int bgRes = 0;
        switch (type) {
            case Swipeable.DRAWABLE_SWIPE_LEFT_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_left;
                break;
            case Swipeable.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_right;
                break;
        }

        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override // DraggableItemAdapter
    public boolean onCheckCanStartDrag(MainViewHolder holder, int position, int x, int y) {
        //Log.d(TAG, "onCheckCanStartDrag");

        // This prevents drag from initiating while swiping
        if(mSwiping){
            mSwiping = false;
            return false;

        // This prevents drag from initiating if you swipe the screen immediately after a full
        // swipe action has been performed.
        } else if(mFullSwipePerformed){
            mFullSwipePerformed = false;
            return false;
        }

        return true;
    }

    @Override // DraggableItemAdapter
    public ItemDraggableRange onGetItemDraggableRange(MainViewHolder holder, int position) {
        //Log.d(TAG, "onGetItemDraggableRange");
        return null;
    }

    @Override // DraggableItemAdapter
    public void onMoveItem(int fromPosition, int toPosition) {
        //Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

        if (fromPosition == toPosition) {
            return;
        }

        mListManipulator.moveItem(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void removeListManipulator(){
        mListManipulator = null;
    }

    private static class SwipeResultAction extends SwipeResultActionRemoveItem {
        private MainAdapter mAdapter;
        private MainViewHolder mHolder;
        private final int mPosition;

        SwipeResultAction(MainAdapter adapter, MainViewHolder holder, int position) {
            mAdapter = adapter;
            mPosition = position;
            mHolder = holder;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();
            //Log.d(TAG, "onPerformAction");
            mAdapter.mFullSwipePerformed = true;
            mAdapter.mListManipulator.removeItem(mPosition);
            mAdapter.notifyItemRemoved(mPosition);
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();
            //Log.d(TAG, "onSlideAnimationEnd");
            mAdapter.mEventListener.onItemRemoved(mHolder);
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            //Log.d(TAG, "onCleanUp");
            mHolder = null;
            mAdapter = null;
        }
    }
}
