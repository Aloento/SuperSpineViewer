package com.esotericsoftware.spine36.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.esotericsoftware.spine36.AnimationState;
import com.esotericsoftware.spine36.Skeleton;
import com.esotericsoftware.spine36.SkeletonRenderer;

/** A scene2d drawable that draws a skeleton. The animation state and skeleton must be updated each frame, or
 * {@link #update(float)} called each frame. */
public class SkeletonDrawable extends BaseDrawable {
	private SkeletonRenderer renderer;
	private Skeleton skeleton;
	AnimationState state;

	/** Creates an uninitialized SkeletonDrawable. The renderer, skeleton, and animation state must be set before use. */
	public SkeletonDrawable () {
	}

	public SkeletonDrawable (SkeletonRenderer renderer, Skeleton skeleton, AnimationState state) {
		this.renderer = renderer;
		this.skeleton = skeleton;
		this.state = state;
	}

	public void update (float delta) {
		state.update(delta);
		state.apply(skeleton);
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		skeleton.setPosition(x, y);
		skeleton.updateWorldTransform();
		renderer.draw(batch, skeleton);
	}

	public SkeletonRenderer getRenderer () {
		return renderer;
	}

	public void setRenderer (SkeletonRenderer renderer) {
		this.renderer = renderer;
	}

	public Skeleton getSkeleton () {
		return skeleton;
	}

	public void setSkeleton (Skeleton skeleton) {
		this.skeleton = skeleton;
	}

	public AnimationState getAnimationState () {
		return state;
	}

	public void setAnimationState (AnimationState state) {
		this.state = state;
	}
}
