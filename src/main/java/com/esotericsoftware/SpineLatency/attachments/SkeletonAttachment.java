package com.esotericsoftware.SpineLatency.attachments;

import com.esotericsoftware.SpineLatency.Skeleton;

public class SkeletonAttachment extends Attachment {
    private Skeleton skeleton;

    public SkeletonAttachment(String name) {
        super(name);
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }
}
