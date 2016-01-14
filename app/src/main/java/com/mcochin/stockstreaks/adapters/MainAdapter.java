package com.mcochin.stockstreaks.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
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
import com.mcochin.stockstreaks.custom.MyApplication;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.fragments.ListManagerFragment;
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

    private ListManagerFragment mListFragment;
    private ListManipulator mListManipulator;
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
            mProgressWheel = itemView.findViewById(R.id.progress_wheel_load_a_few);
            mRetryButton = itemView.findViewById(R.id.button_retry);

            mContainer.setOnClickListener(this);
            mContainer.setOnTouchListener(this);
            if(mRetryButton != null) {
                //first_item will not have a retry button/progress wheel
                mRetryButton.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if (mEventListener != null) {
                int id = v.getId();

                switch (id) {
                    case R.id.swipe_container:
                        mEventListener.onItemClick(this);
                        break;

                    case R.id.button_retry:
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
                        ListManagerFragment listFragment, EventListener eventListener){

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
        String symbol = stock.getSymbol();

        // Determine if this is a dummy "loading" item
        if(symbol.equals(ListManipulator.LOADING_ITEM)) {
            holder.mContainer.setVisibility(View.INVISIBLE);

            if(mListFragment.isLoadingMore()){
                holder.mProgressWheel.setVisibility(View.VISIBLE);
                holder.mRetryButton.setVisibility(View.INVISIBLE);

            }else{
                holder.mProgressWheel.setVisibility(View.INVISIBLE);
                holder.mRetryButton.setVisibility(View.VISIBLE);
            }
        }else {
            holder.mContainer.setVisibility(View.VISIBLE);
            if(position != 0){
                holder.mProgressWheel.setVisibility(View.INVISIBLE);
                holder.mRetryButton.setVisibility(View.INVISIBLE);
            }

            Resources resources = mContext.getResources();

            String fullName = stock.getFullName();
            String recentClose = resources.getString(
                    R.string.placeholder_dollar,
                    Utility.roundTo2StringDecimals(stock.getRecentClose()));
            float changeDollar = stock.getChangeDollar();
            float changePercent = stock.getChangePercent();
            int streak = stock.getStreak();

            // Set symbol, full name, and recentClose
            holder.mSymbol.setText(symbol);
            holder.mFullName.setText(fullName);
            holder.mRecentClose.setText(recentClose);

            // Determine the color and the arrow image of the changes
            Pair<Integer, Integer> changeColorAndDrawableIds =
                    Utility.getChangeColorAndArrowDrawableIds(changeDollar);
            int color = ContextCompat.getColor(mContext, changeColorAndDrawableIds.first);
            holder.mStreakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

            // Format dollar/percent change float values to 2 decimals
            String changeDollarFormat = resources.getString(
                    R.string.placeholder_dollar,
                    Utility.roundTo2StringDecimals(changeDollar));

            String changePercentFormat = resources.getString(
                    R.string.placeholder_percent,
                    Utility.roundTo2StringDecimals(changePercent));

            // Set our updateTime, dollar/percent change, change color, and streak
            if (position == 0 && resources.getBoolean(R.bool.is_phone)) { //list_first_item
                Date updateTime = Utility.getLastUpdateTime(mContext.getContentResolver()).getTime();
                SimpleDateFormat sdf = new SimpleDateFormat(resources.getString(
                        R.string.update_time_format_wide), Locale.US);
                holder.mUpdateTime.setText(resources.getString(R.string.placeholder_update_time,
                        sdf.format(updateTime)));

                holder.mChangeDollar.setText(changeDollarFormat);
                holder.mChangeDollar.setTextColor(color);

                holder.mChangePercent.setText(changePercentFormat);
                holder.mChangePercent.setTextColor(color);

                holder.mStreak.setText(mContext.getString(Math.abs(streak) == 1 ?
                                R.string.placeholder_day : R.string.placeholder_days, streak));

            } else { //list_item
                holder.mChangeAmt.setText(resources.getString(
                        R.string.placeholder_change_amt, changeDollarFormat, changePercentFormat));
                holder.mChangeAmt.setTextColor(color);
                holder.mStreak.setText(mContext.getString(R.string.placeholder_d, streak));
            }



            // Set background resource (target view ID: container)
            final int dragState = holder.getDragStateFlags();
            if (((dragState & Draggable.STATE_FLAG_IS_UPDATED) != 0)) {
                int bgResId;

                // We can't default bgResId to 0 because of this super rare case that removes the
                // background if it is left at 0.
                if (position == 0 && mIsPhone) {
                    bgResId = R.drawable.list_item_first_selector;

                } else {
                    bgResId = R.drawable.list_item_selector;
                }
                // ACTIVE flags is the one being acted upon
                if ((dragState & Draggable.STATE_FLAG_IS_ACTIVE) != 0) {
                    bgResId = R.drawable.bg_item_dragging_active_state;
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
        if (mListManipulator.getItem(position).getSymbol().equals(ListManipulator.LOADING_ITEM)
                || MyApplication.getInstance().isRefreshing()) {
            return SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_ANY;
        }
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
        return !mListManipulator.getItem(position).getSymbol().equals(ListManipulator.LOADING_ITEM);
    }

    @Override // DraggableItemAdapter
    public ItemDraggableRange onGetItemDraggableRange(MainViewHolder holder, int position) {
        //Log.d(TAG, "onGetItemDraggableRange");
        return null;
    }

    @Override // DraggableItemAdapter
    public void onMoveItem(int fromPosition, int toPosition) {
        //Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");
        if (mListManipulator.getItem(toPosition).getSymbol().equals(ListManipulator.LOADING_ITEM)
                || fromPosition == toPosition
                || MyApplication.getInstance().isRefreshing()) {
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
