package com.esotericsoftware.SpineStandard.utils;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.esotericsoftware.SpineStandard.AnimationState;
import com.esotericsoftware.SpineStandard.Skeleton;
import com.esotericsoftware.SpineStandard.SkeletonRenderer;

public class SkeletonDrawable extends BaseDrawable {
    AnimationState state;
    private SkeletonRenderer renderer;
    private Skeleton skeleton;
    private boolean resetBlendFunction = true;

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
        skeleton.setPosition(x, y);
        skeleton.updateWorldTransform();
        renderer.draw(batch, skeleton);

        switch (RuntimesLoader.spineVersion.get()) {
            case 38, 37 -> {
                int blendSrc = batch.getBlendSrcFunc(), blendDst = batch.getBlendDstFunc();
                int blendSrcAlpha = batch.getBlendSrcFuncAlpha(), blendDstAlpha = batch.getBlendDstFuncAlpha();
                if (resetBlendFunction) batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
            }
        }
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


    public void setResetBlendFunction(boolean resetBlendFunction) {
        this.resetBlendFunction = resetBlendFunction;
    }
}
