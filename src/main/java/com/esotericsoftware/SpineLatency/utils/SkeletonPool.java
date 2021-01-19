package com.esotericsoftware.SpineLatency.utils;

import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.SpineLatency.Skeleton;
import com.esotericsoftware.SpineLatency.SkeletonData;

public class SkeletonPool extends Pool<Skeleton> {
    private final SkeletonData skeletonData;

    public SkeletonPool(SkeletonData skeletonData) {
        this.skeletonData = skeletonData;
    }

    public SkeletonPool(SkeletonData skeletonData, int initialCapacity) {
        super(initialCapacity);
        this.skeletonData = skeletonData;
    }

    public SkeletonPool(SkeletonData skeletonData, int initialCapacity, int max) {
        super(initialCapacity, max);
        this.skeletonData = skeletonData;
    }

    protected Skeleton newObject() {
        return new Skeleton(skeletonData);
    }
}
