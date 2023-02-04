package com.esotericsoftware.Spine40.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.esotericsoftware.Spine40.AnimationState;
import com.esotericsoftware.Spine40.Skeleton;
import com.esotericsoftware.Spine40.SkeletonRenderer;

/**
 * A scene2d drawable that draws a skeleton. The animation state and skeleton must be updated each frame, or
 * {@link #update(float)} called each frame.
 */
public class SkeletonDrawable extends BaseDrawable {
    AnimationState state;
    private SkeletonRenderer renderer;
    private Skeleton skeleton;
    private boolean resetBlendFunction = true;

    /**
     * Creates an uninitialized SkeletonDrawable. The renderer, skeleton, and animation state must be set before use.
     */
    public SkeletonDrawable() {
    }

    public SkeletonDrawable(SkeletonRenderer renderer, Skeleton skeleton, AnimationState state) {
        this.renderer = renderer;
        this.skeleton = skeleton;
        this.state = state;
    }

    public void update(float delta) {
        state.update(delta);
        state.apply(skeleton);
    }

    public void draw(Batch batch, float x, float y, float width, float height) {
        int blendSrc = batch.getBlendSrcFunc(), blendDst = batch.getBlendDstFunc();
        int blendSrcAlpha = batch.getBlendSrcFuncAlpha(), blendDstAlpha = batch.getBlendDstFuncAlpha();

        skeleton.setPosition(x, y);
        skeleton.updateWorldTransform();
        renderer.draw(batch, skeleton);

        if (resetBlendFunction) batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
    }

    public SkeletonRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(SkeletonRenderer renderer) {
        this.renderer = renderer;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public AnimationState getAnimationState() {
        return state;
    }

    public void setAnimationState(AnimationState state) {
        this.state = state;
    }

    public boolean getResetBlendFunction() {
        return resetBlendFunction;
    }

    /**
     * If false, the blend function will be left as whatever {@link SkeletonRenderer#draw(Batch, Skeleton)} set. This can reduce
     * batch flushes in some cases, but means other rendering may need to first set the blend function. Default is true.
     */
    public void setResetBlendFunction(boolean resetBlendFunction) {
        this.resetBlendFunction = resetBlendFunction;
    }
}
