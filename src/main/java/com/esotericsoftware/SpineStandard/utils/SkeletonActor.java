package com.esotericsoftware.SpineStandard.utils;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.esotericsoftware.SpineStandard.AnimationState;
import com.esotericsoftware.SpineStandard.Skeleton;
import com.esotericsoftware.SpineStandard.SkeletonRenderer;

public class SkeletonActor extends Actor {
    AnimationState state;
    private SkeletonRenderer renderer;
    private Skeleton skeleton;
    private boolean resetBlendFunction = true;

    public SkeletonActor() {
    }

    public SkeletonActor(SkeletonRenderer renderer, Skeleton skeleton, AnimationState state) {
        this.renderer = renderer;
        this.skeleton = skeleton;
        this.state = state;
    }

    public void act(float delta) {
        state.update(delta);
        state.apply(skeleton);
        super.act(delta);
    }

    public void draw(Batch batch, float parentAlpha) {
        Color color = skeleton.getColor();
        float oldAlpha = color.a;
        skeleton.getColor().a *= parentAlpha;

        skeleton.setPosition(getX(), getY());
        skeleton.updateWorldTransform();
        renderer.draw(batch, skeleton);

        if (RuntimesLoader.spineVersion.get() > 36) {
            int blendSrc = batch.getBlendSrcFunc(), blendDst = batch.getBlendDstFunc();
            int blendSrcAlpha = batch.getBlendSrcFuncAlpha(), blendDstAlpha = batch.getBlendDstFuncAlpha();
            if (resetBlendFunction)
                batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
        }
        color.a = oldAlpha;
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
}
