package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;

public class PathConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array();
    SlotData target;
    PositionMode positionMode;
    SpacingMode spacingMode;
    RotateMode rotateMode;
    float offsetRotation;
    float position, spacing, mixRotate, mixX, mixY;

    public PathConstraintData(String name) {
        super(name);
    }

    public Array<BoneData> getBones() {
        return bones;
    }

    public SlotData getTarget() {
        return target;
    }

    public void setTarget(SlotData target) {
        if (target == null) throw new IllegalArgumentException("target cannot be null.");
        this.target = target;
    }

    public PositionMode getPositionMode() {
        return positionMode;
    }

    public void setPositionMode(PositionMode positionMode) {
        if (positionMode == null) throw new IllegalArgumentException("positionMode cannot be null.");
        this.positionMode = positionMode;
    }

    public SpacingMode getSpacingMode() {
        return spacingMode;
    }

    public void setSpacingMode(SpacingMode spacingMode) {
        if (spacingMode == null) throw new IllegalArgumentException("spacingMode cannot be null.");
        this.spacingMode = spacingMode;
    }

    public RotateMode getRotateMode() {
        return rotateMode;
    }

    public void setRotateMode(RotateMode rotateMode) {
        if (rotateMode == null) throw new IllegalArgumentException("rotateMode cannot be null.");
        this.rotateMode = rotateMode;
    }

    public float getOffsetRotation() {
        return offsetRotation;
    }

    public void setOffsetRotation(float offsetRotation) {
        this.offsetRotation = offsetRotation;
    }

    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    public float getSpacing() {
        return spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
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

    public enum PositionMode {
        fixed, percent;
        static public final PositionMode[] values = PositionMode.values();
    }

    public enum SpacingMode {
        length, fixed, percent, proportional;
        static public final SpacingMode[] values = SpacingMode.values();
    }

    public enum RotateMode {
        tangent, chain, chainScale;
        static public final RotateMode[] values = RotateMode.values();
    }
}
