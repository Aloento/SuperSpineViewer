package com.esotericsoftware.SpinePreview.attachments;

import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.SpinePreview.Skeleton;


public class SkeletonAttachment extends Attachment {
    private @Null
    Skeleton skeleton;

    public SkeletonAttachment(String name) {
        super(name);
    }


    public Skeleton getSkeleton() {
        return skeleton;
    }

    public void setSkeleton(@Null Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public Attachment copy() {
        SkeletonAttachment copy = new SkeletonAttachment(name);
        copy.skeleton = skeleton;
        return copy;
    }
}
