package com.mcochin.stockstreaks.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.fragments.ListManagerFragment;

/**
 * Created by Marco on 1/28/2016.
 */
public class SortDialog extends DialogFragment {
    public static final String TAG = SortDialog.class.getSimpleName();
    private OnSortFinishedListener mSortListener;

    public interface OnSortFinishedListener{
        void onSortFinished();
    }

    public static SortDialog newInstance(OnSortFinishedListener listener) {
        SortDialog fragment = new SortDialog();
        fragment.setOnSortFinishedListener(listener);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.overflow_sort)
                .customView(R.layout.dialog_custom_sort, true)
                .positiveText(R.string.overflow_sort)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (dialog.getCustomView() != null) {
                            RadioGroup sortType = (RadioGroup) dialog.getCustomView()
                                    .findViewById(R.id.radio_group_sort_type);

                            int type;
                            switch (sortType.getCheckedRadioButtonId()) {
                                case R.id.sort_alphabetical:
                                    type = ListManipulator.SORT_ALPHABETICAL;
                                    break;
                                case R.id.sort_streak:
                                    type = ListManipulator.SORT_STREAK;
                                    break;
                                case R.id.sort_dollar_change:
                                    type = ListManipulator.SORT_CHANGE_DOLLAR;
                                    break;
                                case R.id.sort_percent_change:
                                    type = ListManipulator.SORT_CHANGE_PERCENT;
                                    break;
                                case R.id.sort_recent_close:
                                    type = ListManipulator.SORT_RECENT_CLOSE;
                                    break;
                                default:
                                    type = ListManipulator.SORT_ALPHABETICAL;
                            }

                            RadioGroup sortOrder = (RadioGroup) dialog.getCustomView()
                                    .findViewById(R.id.radio_group_sort_order);

                            int order;
                            switch (sortOrder.getCheckedRadioButtonId()) {
                                case R.id.sort_ascending:
                                    order = ListManipulator.SORT_ASC;
                                    break;
                                case R.id.sort_descending:
                                    order = ListManipulator.SORT_DESC;
                                    break;
                                default:
                                    order = ListManipulator.SORT_ASC;
                            }

                            ((ListManagerFragment) getActivity().getSupportFragmentManager()
                                    .findFragmentByTag(ListManagerFragment.TAG))
                                    .getListManipulator()
                                    .sort(type | order);

                            if(mSortListener != null){
                                mSortListener.onSortFinished();
                            }
                        }
                    }
                }).build();
    }

    private void setOnSortFinishedListener(OnSortFinishedListener listener){
        mSortListener = listener;
    }
}
