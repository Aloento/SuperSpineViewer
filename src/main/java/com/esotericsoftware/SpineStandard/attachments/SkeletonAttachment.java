package com.esotericsoftware.SpineStandard.attachments;

import Skeleton;

/**
 * Attachment that displays a skeleton.
 */
public class SkeletonAttachment extends Attachment {
    private Skeleton skeleton;

    public SkeletonAttachment(String name) {
        super(name);
    }

    /**
     * @return May return null.
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * @param skeleton May be null.
     */
    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public Attachment copy() {
        SkeletonAttachment copy = new SkeletonAttachment(name);
        copy.skeleton = skeleton;
        return copy;
    }
}
