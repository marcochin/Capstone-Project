package com.mcochin.stockstreakz.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mcochin.stockstreakz.R;
import com.mcochin.stockstreakz.adapters.MainAdapter;

/**
 * Created by Marco on 10/22/2015.
 */
public class MainFragment extends Fragment {
    static final String[] FRUITS = new String[] { "Apple", "Avocado", "Banana",
            "Blueberry", "Coconut", "Durian", "Guava", "Kiwi", "Jackfruit", "Mango",
            "Olive", "Pear", "Sugar-apple", "Orange", "Strawberry", "Pineapple",
            "Watermelon", "Grape", "PassionFruit", "DragonFruit", "Honey-dew",
            "Cantaloupe", "Papaya"};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new MainAdapter(FRUITS));
    }
}
