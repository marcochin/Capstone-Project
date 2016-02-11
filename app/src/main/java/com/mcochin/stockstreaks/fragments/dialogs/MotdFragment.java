package com.mcochin.stockstreaks.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.custom.MyApplication;

/**
 * Created by Marco on 2/8/2016.
 */
public class MotdFragment extends DialogFragment{
    public static final String TAG = MotdFragment.class.getSimpleName();

    private MaterialDialog mMotdDialog;
    private TextView mTextMotd;
    private View mProgressWheel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMotdDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.navigation_motd)
                .customView(R.layout.dialog_custom_motd, true)
                .positiveText(R.string.dialog_close)
                .build();

        View customView = mMotdDialog.getCustomView();
        if(customView != null) {
            mTextMotd = (TextView) customView.findViewById(R.id.text_motd);
            mProgressWheel = customView.findViewById(R.id.progress_wheel);
        }

        updateMsgOfTheDay();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mMotdDialog;
    }

    private void updateMsgOfTheDay(){
        ContainerHolder containerHolder = MyApplication.getInstance().getContainerHolder();
        if(containerHolder != null) {
            // Make links such as: <a href="http://www.google.com">Go to Google</a> work
            //http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            Spanned text = Html.fromHtml(containerHolder.getContainer()
                    .getString(getString(R.string.tag_manager_motd_key)));
            URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);
            SpannableString buffer = new SpannableString(text);
            Linkify.addLinks(buffer, Linkify.ALL);

            for (URLSpan span : currentSpans) {
                int end = text.getSpanEnd(span);
                int start = text.getSpanStart(span);
                buffer.setSpan(span, start, end, 0);
            }
            mTextMotd.setText(buffer);
            mTextMotd.setMovementMethod(LinkMovementMethod.getInstance());

        }else{
            mTextMotd.setText(getString(R.string.dialog_motd));
        }

        mProgressWheel.setVisibility(View.INVISIBLE);
    }
}
