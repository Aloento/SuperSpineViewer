package com.esotericsoftware.SpineLegacy.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.esotericsoftware.SpineLegacy.AnimationState;
import com.esotericsoftware.SpineLegacy.Skeleton;
import com.esotericsoftware.SpineLegacy.SkeletonRenderer;

public class SkeletonActor extends Actor {
    AnimationState state;
    private SkeletonRenderer renderer;
    private Skeleton skeleton;

    public SkeletonActor() {
    }

    public void act(float delta) {
        state.update(delta);
        state.apply(skeleton);
        skeleton.updateWorldTransform();
        super.act(delta);
    }

    public void draw(Batch batch, float parentAlpha) {
        Color color = skeleton.getColor();
        float oldAlpha = color.a;
        skeleton.getColor().a *= parentAlpha;
        skeleton.setPosition(getX(), getY());
        renderer.draw(batch, skeleton);
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
