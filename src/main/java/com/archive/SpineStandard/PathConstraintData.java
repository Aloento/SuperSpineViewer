package com.archive.SpineStandard;

import com.badlogic.gdx.utils.Array;

public class PathConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array<>();
    SlotData target;
    PositionMode positionMode;
    SpacingMode spacingMode;
    RotateMode rotateMode;
    float offsetRotation;
    float position, spacing, rotateMix, translateMix;

    public PathConstraintData(String name) {
        super(name);
    }

    // public Array<BoneData> getBones() {
    //     return bones;
    // }

    // public SlotData getTarget() {
    //     return target;
    // }

    // public void setTarget(SlotData target) {
    //     if (target == null) throw new IllegalArgumentException("target cannot be null.");
    //     this.target = target;
    // }

    // public float getOffsetRotation() {
    //     return offsetRotation;
    // }

    // public void setOffsetRotation(float offsetRotation) {
    //     this.offsetRotation = offsetRotation;
    // }

    // public float getPosition() {
    //     return position;
    // }

    // public void setPosition(float position) {
    //     this.position = position;
    // }

    // public float getSpacing() {
    //     return spacing;
    // }

    // public void setSpacing(float spacing) {
    //     this.spacing = spacing;
    // }

    // public float getRotateMix() {
    //     return rotateMix;
    // }

    // public void setRotateMix(float rotateMix) {
    //     this.rotateMix = rotateMix;
    // }

    // public float getTranslateMix() {
    //     return translateMix;
    // }

    // public void setTranslateMix(float translateMix) {
    //     this.translateMix = translateMix;
    // }

    public enum PositionMode {
        fixed, percent;
        static public final PositionMode[] values = PositionMode.values();
    }

    public enum SpacingMode {
        length, fixed, percent;
        static public final SpacingMode[] values = SpacingMode.values();
    }

    public enum RotateMode {
        tangent, chain, chainScale;
        static public final RotateMode[] values = RotateMode.values();
    }
}
