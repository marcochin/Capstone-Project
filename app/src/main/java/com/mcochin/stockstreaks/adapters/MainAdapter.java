package com.mcochin.stockstreaks.adapters;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;
import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.ListManipulator;

/**
 * This is the adapter for <code>MainFragment</code>
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainViewHolder>
        implements DraggableItemAdapter<MainAdapter.MainViewHolder>,
        SwipeableItemAdapter<MainAdapter.MainViewHolder> {

    public static final String TAG = MainAdapter.class.getSimpleName();
    public static final int LIST_ITEM_NORMAL = 0;
    public static final int LIST_ITEM_FIRST = 1;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private RecyclerViewDragDropManager mDragDropManager;

    /**
     * On devices < KITKAT, the list items lost their padding for some reason. Read somewhere
     * it has to do dynamically changing bg resources. We need to save it and restore it onBind().
     */
    //http://stackoverflow.com/questions/10095196/whered-padding-go-when-setting-background-drawable
    private int mListItemVerticalPadding;
    private int mListItemHorizontalPadding;

    // Our ViewHolder class
    public class MainViewHolder extends AbstractDraggableSwipeableItemViewHolder
            implements View.OnClickListener, View.OnTouchListener {
        TextView mSymbol;
        TextView mFullName;
        TextView mPrevClose;
        TextView mChangeDollar;
        TextView mChangePercent;
        TextView mStreak;
        ViewGroup mContainer;

        public MainViewHolder(View itemView) {
            super(itemView);
            mContainer = (ViewGroup) itemView.findViewById(R.id.swipe_container);
            mSymbol = (TextView) itemView.findViewById(R.id.text_symbol);
            mFullName = (TextView) itemView.findViewById(R.id.text_full_name);
            mPrevClose = (TextView) itemView.findViewById(R.id.text_prev_close);
            mChangeDollar = (TextView) itemView.findViewById(R.id.text_change_dollar);
            mChangePercent = (TextView) itemView.findViewById(R.id.text_change_percent);
            mStreak = (TextView) itemView.findViewById(R.id.text_streak);
            itemView.setOnClickListener(this);
            itemView.setOnTouchListener(this);
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
            if(mEventListener != null) {
                mEventListener.onItemClick(this);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_CANCEL){
                if(mDragDropManager != null && !mDragDropManager.isDragging()) {
                    //Log.d(TAG, "cancelDrag");
                    mDragDropManager.cancelDrag();
                }
            }
            return false;
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
    public MainAdapter (Context context, EventListener eventListener,
                        RecyclerViewDragDropManager dragDropManager, ListManipulator listManipulator){

        mEventListener = eventListener;
        mDragDropManager = dragDropManager;
        mListManipulator = listManipulator;
        mListManipulator.setCursor(null); //TODO remove this, only for debugging

        mListItemVerticalPadding = context.getResources()
                .getDimensionPixelSize(R.dimen.list_item_vertical_padding);
        mListItemHorizontalPadding = context.getResources()
                .getDimensionPixelSize(R.dimen.list_item_horizontal_padding);

        // DraggableItemAdapter and SwipeableItemAdapter require stable IDs, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        int layoutId = viewType == LIST_ITEM_FIRST ? R.layout.list_item_first : R.layout.list_item;

        View v = inflater.inflate(layoutId, parent, false);
        return new MainViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MainViewHolder holder, int position) {
        Log.d(TAG, "onBind");
//        Stock stock = mListManipulator.getItem(position);
//
//        holder.mSymbol.setText(stock.getSymbol());
//        holder.mFullName.setText(stock.getFullName());
//        holder.mPrevClose.setText(String.format("%.2f", stock.getPrevClose()));
//        holder.mChangeDollar.setText(String.format("%.2f", stock.getChangeDollar()));
//        holder.mChangePercent.setText(String.format("%.2f", stock.getChangePercent()));
//        holder.mStreak.setText(String.format("%d", stock.getStreak()));

        // set background resource (target view ID: container)
        final int dragState = holder.getDragStateFlags();

        if (((dragState & Draggable.STATE_FLAG_IS_UPDATED) != 0)){
            int bgResId;

            // ACTIVE flags is the one being acted upon
            if ((dragState & Draggable.STATE_FLAG_IS_ACTIVE) != 0) {
                bgResId = R.drawable.bg_item_dragging_active_state;
            } else if (position == 0){
                bgResId = R.drawable.list_item_first_selector;
            } else{
                bgResId = R.drawable.list_item_selector;
            }

            holder.mContainer.setBackgroundResource(bgResId);
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            holder.mContainer.setPadding(mListItemHorizontalPadding, mListItemVerticalPadding,
                    mListItemHorizontalPadding, mListItemVerticalPadding);
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
    public int getItemViewType(int position) {
        return position == 0 ? LIST_ITEM_FIRST : LIST_ITEM_NORMAL;
    }

    @Override
    public long getItemId(int position) {
        return mListManipulator.getItem(position).getId();
    }

    @Override // SwipeableItemAdapter
    public SwipeResultAction onSwipeItem(MainViewHolder holder, int position, int result) {
        Log.d(TAG, "onSwipeItem(position = " + position + ", result = " + result + ")");

        switch (result) {
            // swipe right
            case Swipeable.RESULT_SWIPED_RIGHT:
                //Log.d(TAG, "Swiped right");
            // swipe left
            case Swipeable.RESULT_SWIPED_LEFT:
                //Log.d(TAG, "Swiped left");
                return new SwipeResultAction(this, holder, position);
            case Swipeable.RESULT_CANCELED:
                // other --- do nothing
            default:
                return null;
        }
    }

    @Override // SwipeableItemAdapter
    public int onGetSwipeReactionType(MainViewHolder holder, int position, int x, int y) {
        Log.d(TAG, "onGetSwipeReactionType");

        // THis is what enables swiping
        return Swipeable.REACTION_CAN_SWIPE_BOTH_H;
    }

    @Override // SwipeableItemAdapter
    public void onSetSwipeBackground(MainViewHolder holder, int position, int type) {
        //Log.d(TAG, "onSetSwipeBackground");

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

        // This is what enables dragging
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

    public void release(){
        mDragDropManager = null;
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
            mAdapter.mListManipulator.removeItem(mPosition);
            mAdapter.notifyItemRemoved(mPosition);
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();
            //Log.d(TAG, "onSlideAnimationEnd");
            if(mAdapter.mEventListener != null) {
                mAdapter.mEventListener.onItemRemoved(mHolder);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            //Log.d(TAG, "onCleanUp");
            // clear the references
            mHolder = null;
            mAdapter = null;
        }
    }
}
