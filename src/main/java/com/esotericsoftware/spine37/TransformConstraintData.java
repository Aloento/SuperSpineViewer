package com.esotericsoftware.spine37;

import com.badlogic.gdx.utils.Array;

public class TransformConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    int order;
    BoneData target;
    float rotateMix, translateMix, scaleMix, shearMix;
    float offsetRotation, offsetX, offsetY, offsetScaleX, offsetScaleY, offsetShearY;
    boolean relative, local;

    public TransformConstraintData(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    
    public String getName() {
        return name;
    }

    
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
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

    
    public float getRotateMix() {
        return rotateMix;
    }

    public void setRotateMix(float rotateMix) {
        this.rotateMix = rotateMix;
    }

    
    public float getTranslateMix() {
        return translateMix;
    }

    public void setTranslateMix(float translateMix) {
        this.translateMix = translateMix;
    }

    
    public float getScaleMix() {
        return scaleMix;
    }

    public void setScaleMix(float scaleMix) {
        this.scaleMix = scaleMix;
    }

    
    public float getShearMix() {
        return shearMix;
    }

    public void setShearMix(float shearMix) {
        this.shearMix = shearMix;
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

    public String toString() {
        return name;
    }
}
