package com.mcochin.stockstreaks.custom;

/**
 * Triple like {@link android.util.Pair} is a container to ease passing around a tuple of three
 * objects.
 */
public class Triple<F, S, T> {
    public final F first;
    public final S second;
    public final T third;

    /**
     * Constructor for a Triple.
     *
     * @param first the first object in the triple
     * @param second the second object in the triple
     * @param third the third object in the triple
     */
    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}
