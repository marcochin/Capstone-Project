package com.mcochin.stockstreaks.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.mcochin.stockstreaks.fragments.ListManipulatorFragment;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private ListManipulatorFragment mListFragment;
    private EventListener mEventListener;
    private RecyclerViewDragDropManager mDragDropManager;
    private Context mContext;

    /**
     * On devices < KITKAT, the list items lost their padding for some reason. Read somewhere
     * it has to do dynamically changing bg resources. We need to save it and restore it onBind().
     */
    //http://stackoverflow.com/questions/10095196/whered-padding-go-when-setting-background-drawable
    private int mListItemVerticalPadding;
    private int mListItemHorizontalPadding;
    private int mListItemFirstPadding;

    private boolean mIsPhone;

    // Interfaces
    public interface EventListener {
        void onItemClick(MainViewHolder holder);
        void onItemRemoved(MainViewHolder holder);
        void onItemMoved(int fromPosition, int toPosition);
        void onItemRetryClick(MainViewHolder holder);
    }

    // NOTE: Make accessible with shorter name
    private interface Draggable extends DraggableItemConstants {
    }
    private interface Swipeable extends SwipeableItemConstants {
    }

    // Our ViewHolder class
    public class MainViewHolder extends AbstractDraggableSwipeableItemViewHolder
            implements View.OnClickListener, View.OnTouchListener {
        ViewGroup mContainer;
        TextView mUpdateTime;
        TextView mSymbol;
        TextView mFullName;
        TextView mRecentClose;
        TextView mChangeDollar;
        TextView mChangePercent;
        TextView mChangeAmt;
        TextView mStreak;
        ImageView mStreakArrow;
        View mProgressWheel;
        View mRetryButton;


        public MainViewHolder(View itemView) {
            super(itemView);
            mContainer = (ViewGroup) itemView.findViewById(R.id.swipe_container);
            mUpdateTime = (TextView)itemView.findViewById(R.id.text_update_time);
            mSymbol = (TextView) itemView.findViewById(R.id.text_symbol);
            mFullName = (TextView) itemView.findViewById(R.id.text_full_name);
            mRecentClose = (TextView) itemView.findViewById(R.id.text_recent_close);
            mChangeDollar = (TextView) itemView.findViewById(R.id.text_change_dollar);
            mChangePercent = (TextView) itemView.findViewById(R.id.text_change_percent);
            mChangeAmt = (TextView) itemView.findViewById(R.id.text_change_amt);
            mStreak = (TextView) itemView.findViewById(R.id.text_streak);
            mStreakArrow = (ImageView)itemView.findViewById(R.id.image_streak_arrow);
            mProgressWheel = itemView.findViewById(R.id.progress_wheel);
            mRetryButton = itemView.findViewById(R.id.retry_button);

            itemView.setOnClickListener(this);
            itemView.setOnTouchListener(this);
            mRetryButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mEventListener != null) {
                int id = v.getId();

                switch (id) {
                    case R.id.swipe_container:
                        mEventListener.onItemClick(this);
                        break;

                case R.id.retry_button:
                    mEventListener.onItemRetryClick(this);
                    break;
                }
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

        @Override
        public View getSwipeableContainerView() {
            return mContainer;
        }

        public CharSequence getSymbol(){
            return mSymbol.getText();
        }
    }

    // Constructor
    public MainAdapter (Context context, RecyclerViewDragDropManager dragDropManager,
                        ListManipulatorFragment listFragment, EventListener eventListener){

        mEventListener = eventListener;
        mDragDropManager = dragDropManager;
        mListFragment = listFragment;
        mListManipulator = listFragment.getListManipulator();
        mContext = context;
//        mListManipulator.setShownListCursor(null); //TODO remove this, only for debugging

        Resources resources = mContext.getResources();
        mListItemVerticalPadding = resources.getDimensionPixelSize(R.dimen.list_item_vertical_padding);
        mListItemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.list_item_horizontal_padding);
        mListItemFirstPadding = resources.getDimensionPixelSize(R.dimen.list_item_first_padding);

        mIsPhone = resources.getBoolean(R.bool.is_phone);

        // DraggableItemAdapter and SwipeableItemAdapter require stable IDs, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        int layoutId = viewType == LIST_ITEM_FIRST && mIsPhone
                ? R.layout.list_item_first : R.layout.list_item;

        View v = inflater.inflate(layoutId, parent, false);
        return new MainViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MainViewHolder holder, int position) {
//        Log.d(TAG, "onBind");
        Stock stock = mListManipulator.getItem(position);

        // Determine if this is a dummy "loading" item
        if(stock.getSymbol().equals(ListManipulator.LOADING_ITEM)) {
            holder.mContainer.setVisibility(View.INVISIBLE);
            if(mListFragment.isLoadingAFew()){
                holder.mProgressWheel.setVisibility(View.VISIBLE);
                holder.mRetryButton.setVisibility(View.INVISIBLE);
            }else{
                holder.mProgressWheel.setVisibility(View.INVISIBLE);
                holder.mRetryButton.setVisibility(View.VISIBLE);
            }

        }else {
            holder.mContainer.setVisibility(View.VISIBLE);
            holder.mProgressWheel.setVisibility(View.INVISIBLE);
            holder.mRetryButton.setVisibility(View.INVISIBLE);
            Resources resources = mContext.getResources();

            // Set symbol
            holder.mSymbol.setText(stock.getSymbol());

            // Set full name
            holder.mFullName.setText(stock.getFullName());

            // Set recent close
            String recentClose = String.format("%.2f", stock.getRecentClose());
            holder.mRecentClose.setText(resources.getString(R.string.placeholder_dollar, recentClose));

            // Format dollar/percent change float values to 2 decimals
            String changeDollar = resources.getString(
                    R.string.placeholder_dollar, String.format("%.2f", stock.getChangeDollar()));

            String changePercent = resources.getString(
                    R.string.placeholder_percent, String.format("%.2f", stock.getChangePercent()));

            // Format streak String
            String streak = String.format("%d", stock.getStreak());

            // Get our dollar/percent change colors and set our stock arrow ImageView
            int color;
            if (stock.getChangeDollar() > 0) {
                color = ContextCompat.getColor(mContext, R.color.stock_up_green);
                holder.mStreakArrow.setBackgroundResource(R.drawable.ic_streak_up);

            } else if (stock.getChangeDollar() < 0) {
                color = ContextCompat.getColor(mContext, R.color.stock_down_red);
                holder.mStreakArrow.setBackgroundResource(R.drawable.ic_streak_down);

            } else {
                color = ContextCompat.getColor(mContext, R.color.stock_neutral);
            }

            // Set our dollar/percent change, change color, and streak
            if (position == 0) { //list_first_item
                SimpleDateFormat sdf = new SimpleDateFormat(resources.getString(
                        R.string.update_time_format_ref),
                        Locale.US);

                Date updateTime = Utility.getLastUpdateTime(mContext.getContentResolver()).getTime();
                holder.mUpdateTime.setText(resources.getString(
                        R.string.placeholder_update_time, sdf.format(updateTime)));

                holder.mChangeDollar.setText(changeDollar);
                holder.mChangeDollar.setTextColor(color);

                holder.mChangePercent.setText(changePercent);
                holder.mChangePercent.setTextColor(color);

                holder.mStreak.setText(mContext.getString(R.string.placeholder_days, streak));

            } else { //list_item
                holder.mChangeAmt.setText(resources.getString(
                        R.string.placeholder_change_amt, changeDollar, changePercent));
                holder.mChangeAmt.setTextColor(color);
                holder.mStreak.setText(mContext.getString(R.string.placeholder_d, streak));
            }

            // Set background resource (target view ID: container)
            final int dragState = holder.getDragStateFlags();
            if (((dragState & Draggable.STATE_FLAG_IS_UPDATED) != 0)) {
                int bgResId;

                // ACTIVE flags is the one being acted upon
                if ((dragState & Draggable.STATE_FLAG_IS_ACTIVE) != 0) {
                    bgResId = R.drawable.bg_item_dragging_active_state;
                } else if (position == 0 && mIsPhone) {
                    bgResId = R.drawable.list_item_first_selector;
                } else {
                    bgResId = R.drawable.list_item_selector;
                }

                holder.mContainer.setBackgroundResource(bgResId);
            }

            // Restore back padding for pre-kitkat list_items
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                if (position == 0 && mIsPhone) {
                    holder.mContainer.setPadding(mListItemFirstPadding, mListItemFirstPadding,
                            mListItemFirstPadding, mListItemFirstPadding);
                } else {
                    holder.mContainer.setPadding(mListItemHorizontalPadding, mListItemVerticalPadding,
                            mListItemHorizontalPadding, mListItemVerticalPadding);
                }
            }

            // Set swiping properties. This sets the horizontal offset of the items
            holder.setSwipeItemHorizontalSlideAmount(0);
        }
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
//        Log.d(TAG, "onSwipeItem(position = " + position + ", result = " + result + ")");

        switch (result) {
            // swipe right
            case Swipeable.RESULT_SWIPED_RIGHT:
                //Log.d(TAG, "Swiped right");
            // swipe left
            case Swipeable.RESULT_SWIPED_LEFT:
                //Log.d(TAG, "Swiped left");
                return new SwipeResultAction(this, holder);
            case Swipeable.RESULT_CANCELED:
                // other --- do nothing
            default:
                return null;
        }
    }

    @Override // SwipeableItemAdapter
    public int onGetSwipeReactionType(MainViewHolder holder, int position, int x, int y) {
//        Log.d(TAG, "onGetSwipeReactionType");

        // This is what enables swiping
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
        if(mEventListener != null){
            mEventListener.onItemMoved(fromPosition, toPosition);
        }
    }

    public void release(){
        mDragDropManager = null;
        mListManipulator = null;
        mListFragment = null;
        mContext = null;
    }

    private static class SwipeResultAction extends SwipeResultActionRemoveItem {
        private MainAdapter mAdapter;
        private MainViewHolder mHolder;

        SwipeResultAction(MainAdapter adapter, MainViewHolder holder) {
            mAdapter = adapter;
            mHolder = holder;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();
            //Log.d(TAG, "onPerformAction");
            if(mAdapter.mEventListener != null) {
                mAdapter.mEventListener.onItemRemoved(mHolder);
            }
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();
            //Log.d(TAG, "onSlideAnimationEnd");
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
