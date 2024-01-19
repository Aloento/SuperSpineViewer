package com.archive.Spine40;

/**
 * The interface for items updated by {@link Skeleton#updateWorldTransform()}.
 */
public interface Updatable {
    void update();

    /**
     * Returns false when this item has not been updated because a skin is required and the {@link Skeleton#getSkin() active skin}
     * does not contain this item.
     *
     * @see Skin#getBones()
     * @see Skin#getConstraints()
     */
    boolean isActive();
}
