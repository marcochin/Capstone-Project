package com.mcochin.stockstreakz.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mcochin.stockstreakz.R;

/**
 * This is the adapter for <code>MainFragment</code>
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainViewHolder> {
    private String[] mData;

    public MainAdapter (String[] data){
        mData = data;
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView mTextView;

        public MainViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text_symbol);
        }
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.list_item, parent, false);
        return new MainViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MainViewHolder holder, int position) {
        holder.mTextView.setText(mData[position]);
    }

    @Override
    public int getItemCount() {
        return mData.length;
    }
}
