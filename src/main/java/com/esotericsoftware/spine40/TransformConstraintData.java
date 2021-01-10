package com.esotericsoftware.spine40;

import com.badlogic.gdx.utils.Array;

/**
 * Stores the setup pose for a {@link TransformConstraint}.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-transform-constraints">Transform constraints</a> in the Spine User Guide.
 */
public class TransformConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array();
    BoneData target;
    float mixRotate, mixX, mixY, mixScaleX, mixScaleY, mixShearY;
    float offsetRotation, offsetX, offsetY, offsetScaleX, offsetScaleY, offsetShearY;
    boolean relative, local;

    public TransformConstraintData(String name) {
        super(name);
    }

    /**
     * The bones that will be modified by this transform constraint.
     */
    public Array<BoneData> getBones() {
        return bones;
    }

    /**
     * The target bone whose world transform will be copied to the constrained bones.
     */
    public BoneData getTarget() {
        return target;
    }

    public void setTarget(BoneData target) {
        if (target == null) throw new IllegalArgumentException("target cannot be null.");
        this.target = target;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained rotation.
     */
    public float getMixRotate() {
        return mixRotate;
    }

    public void setMixRotate(float mixRotate) {
        this.mixRotate = mixRotate;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained translation X.
     */
    public float getMixX() {
        return mixX;
    }

    public void setMixX(float mixX) {
        this.mixX = mixX;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained translation Y.
     */
    public float getMixY() {
        return mixY;
    }

    public void setMixY(float mixY) {
        this.mixY = mixY;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained scale X.
     */
    public float getMixScaleX() {
        return mixScaleX;
    }

    public void setMixScaleX(float mixScaleX) {
        this.mixScaleX = mixScaleX;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained scale Y.
     */
    public float getMixScaleY() {
        return mixScaleY;
    }

    public void setMixScaleY(float mixScaleY) {
        this.mixScaleY = mixScaleY;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained shear Y.
     */
    public float getMixShearY() {
        return mixShearY;
    }

    public void setMixShearY(float mixShearY) {
        this.mixShearY = mixShearY;
    }

    /**
     * An offset added to the constrained bone rotation.
     */
    public float getOffsetRotation() {
        return offsetRotation;
    }

    public void setOffsetRotation(float offsetRotation) {
        this.offsetRotation = offsetRotation;
    }

    /**
     * An offset added to the constrained bone X translation.
     */
    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * An offset added to the constrained bone Y translation.
     */
    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * An offset added to the constrained bone scaleX.
     */
    public float getOffsetScaleX() {
        return offsetScaleX;
    }

    public void setOffsetScaleX(float offsetScaleX) {
        this.offsetScaleX = offsetScaleX;
    }

    /**
     * An offset added to the constrained bone scaleY.
     */
    public float getOffsetScaleY() {
        return offsetScaleY;
    }

    public void setOffsetScaleY(float offsetScaleY) {
        this.offsetScaleY = offsetScaleY;
    }

    /**
     * An offset added to the constrained bone shearY.
     */
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
