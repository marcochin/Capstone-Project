package com.mcochin.stockstreaks.pojos;

import android.support.annotation.NonNull;

import com.mcochin.stockstreaks.utils.Utility;

/**
 * Created by Marco on 2/2/2016.
 */
public class StreakFrequency implements Comparable<StreakFrequency>{

    private int mStreak;
    private int mFrequency;

    public StreakFrequency(int streak, int frequency) {
        mStreak = streak;
        mFrequency = frequency;
    }

    public int getStreak() {
        return mStreak;
    }

    public void setStreak(int streak) {
        mStreak = streak;
    }

    public int getFrequency() {
        return mFrequency;
    }

    public void setFrequency(int frequency) {
        mFrequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof StreakFrequency){
            if(mStreak == ((StreakFrequency)o).getStreak()){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        //http://stackoverflow.com/questions/113511/best-implementation-for-hashcode-method
        // Start with a non-zero constant. Prime is preferred
        int result = 17;

        //For every field f tested in the equals() method, calculate a hash code c by:
        result = 31 * result + mStreak;

        return result;
    }

    @Override
    public int compareTo(@NonNull StreakFrequency another) {
        return Utility.compare(mStreak, another.getStreak());
    }
}
