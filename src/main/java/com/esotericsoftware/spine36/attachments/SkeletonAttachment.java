package com.esotericsoftware.spine36.attachments;

import com.esotericsoftware.spine36.Skeleton;

/** Attachment that displays a skeleton. */
public class SkeletonAttachment extends Attachment {
	private Skeleton skeleton;

	public SkeletonAttachment (String name) {
		super(name);
	}

	/** @return May return null. */
	public Skeleton getSkeleton () {
		return skeleton;
	}

	/** @param skeleton May be null. */
	public void setSkeleton (Skeleton skeleton) {
		this.skeleton = skeleton;
	}
}
