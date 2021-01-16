package com.esotericsoftware.spine37.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.esotericsoftware.spine37.AnimationState;
import com.esotericsoftware.spine37.Skeleton;
import com.esotericsoftware.spine37.SkeletonRenderer;


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
        int blendSrc = batch.getBlendSrcFunc(), blendDst = batch.getBlendDstFunc();
        int blendSrcAlpha = batch.getBlendSrcFuncAlpha(), blendDstAlpha = batch.getBlendDstFuncAlpha();

        Color color = skeleton.getColor();
        float oldAlpha = color.a;
        skeleton.getColor().a *= parentAlpha;

        skeleton.setPosition(getX(), getY());
        skeleton.updateWorldTransform();
        renderer.draw(batch, skeleton);

        if (resetBlendFunction) batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);

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

    public boolean getResetBlendFunction() {
        return resetBlendFunction;
    }

    
    public void setResetBlendFunction(boolean resetBlendFunction) {
        this.resetBlendFunction = resetBlendFunction;
    }
}
