package com.esotericsoftware.spine21.attachments;

import com.esotericsoftware.spine21.Skeleton;

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
