package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;


public class TransformConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array();
    BoneData target;
    float mixRotate, mixX, mixY, mixScaleX, mixScaleY, mixShearY;
    float offsetRotation, offsetX, offsetY, offsetScaleX, offsetScaleY, offsetShearY;
    boolean relative, local;

    public TransformConstraintData(String name) {
        super(name);
    }


    public Array<BoneData> getBones() {
        return bones;
    }


    public BoneData getTarget() {
        return target;
    }

    public void setTarget(BoneData target) {
        if (target == null) throw new IllegalArgumentException("target cannot be null.");
        this.target = target;
    }


    public float getMixRotate() {
        return mixRotate;
    }

    public void setMixRotate(float mixRotate) {
        this.mixRotate = mixRotate;
    }


    public float getMixX() {
        return mixX;
    }

    public void setMixX(float mixX) {
        this.mixX = mixX;
    }


    public float getMixY() {
        return mixY;
    }

    public void setMixY(float mixY) {
        this.mixY = mixY;
    }


    public float getMixScaleX() {
        return mixScaleX;
    }

    public void setMixScaleX(float mixScaleX) {
        this.mixScaleX = mixScaleX;
    }


    public float getMixScaleY() {
        return mixScaleY;
    }

    public void setMixScaleY(float mixScaleY) {
        this.mixScaleY = mixScaleY;
    }


    public float getMixShearY() {
        return mixShearY;
    }

    public void setMixShearY(float mixShearY) {
        this.mixShearY = mixShearY;
    }


    public float getOffsetRotation() {
        return offsetRotation;
    }

    public void setOffsetRotation(float offsetRotation) {
        this.offsetRotation = offsetRotation;
    }


    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }


    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }


    public float getOffsetScaleX() {
        return offsetScaleX;
    }

    public void setOffsetScaleX(float offsetScaleX) {
        this.offsetScaleX = offsetScaleX;
    }


    public float getOffsetScaleY() {
        return offsetScaleY;
    }

    public void setOffsetScaleY(float offsetScaleY) {
        this.offsetScaleY = offsetScaleY;
    }


    public float getOffsetShearY() {
        return offsetShearY;
    }

    public void setOffsetShearY(float offsetShearY) {
        this.offsetShearY = offsetShearY;
    }

    public boolean getRelative() {
        return relative;
    }

    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    public boolean getLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}
