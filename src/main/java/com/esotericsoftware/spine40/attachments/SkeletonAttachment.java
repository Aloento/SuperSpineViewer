package com.esotericsoftware.spine40.attachments;

import com.badlogic.gdx.utils.Null;

import com.esotericsoftware.spine40.Skeleton;

/** Attachment that displays a skeleton. */
public class SkeletonAttachment extends Attachment {
	private @Null Skeleton skeleton;

	public SkeletonAttachment (String name) {
		super(name);
	}

	/** @return May return null. */
	public Skeleton getSkeleton () {
		return skeleton;
	}

	public void setSkeleton (@Null Skeleton skeleton) {
		this.skeleton = skeleton;
	}

	public Attachment copy () {
		SkeletonAttachment copy = new SkeletonAttachment(name);
		copy.skeleton = skeleton;
		return copy;
	}
}
