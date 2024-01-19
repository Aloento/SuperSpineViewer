package com.archive.SpineStandard.attachments;

import com.archive.SpineStandard.Skeleton;

public class SkeletonAttachment extends Attachment {
    private Skeleton skeleton;

    public SkeletonAttachment(String name) {
        super(name);
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    // public void setSkeleton(Skeleton skeleton) {
    //     this.skeleton = skeleton;
    // }

    // public Attachment copy() {
    //     SkeletonAttachment copy = new SkeletonAttachment(name);
    //     copy.skeleton = skeleton;
    //     return copy;
    // }
}
