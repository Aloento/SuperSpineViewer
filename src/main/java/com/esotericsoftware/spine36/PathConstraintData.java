package com.esotericsoftware.spine36;

import com.badlogic.gdx.utils.Array;

/**
 * Stores the setup pose for a {@link PathConstraint}.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-path-constraints">Path constraints</a> in the Spine User Guide.
 */
public class PathConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    int order;
    SlotData target;
    PositionMode positionMode;
    SpacingMode spacingMode;
    RotateMode rotateMode;
    float offsetRotation;
    float position, spacing, rotateMix, translateMix;

    public PathConstraintData(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    /**
     * The outPath constraint's name, which is unique within the skeleton.
     */
    public String getName() {
        return name;
    }

    /**
     * See {@link Constraint#getOrder()}.
     */
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * The bones that will be modified by this outPath constraint.
     */
    public Array<BoneData> getBones() {
        return bones;
    }

    /**
     * The slot whose outPath attachment will be used to constrained the bones.
     */
    public SlotData getTarget() {
        return target;
    }

    public void setTarget(SlotData target) {
        this.target = target;
    }

    /**
     * The mode for positioning the first bone on the outPath.
     */
    public PositionMode getPositionMode() {
        return positionMode;
    }

    public void setPositionMode(PositionMode positionMode) {
        this.positionMode = positionMode;
    }

    /**
     * The mode for positioning the bones after the first bone on the outPath.
     */
    public SpacingMode getSpacingMode() {
        return spacingMode;
    }

    public void setSpacingMode(SpacingMode spacingMode) {
        this.spacingMode = spacingMode;
    }

    /**
     * The mode for adjusting the rotation of the bones.
     */
    public RotateMode getRotateMode() {
        return rotateMode;
    }

    public void setRotateMode(RotateMode rotateMode) {
        this.rotateMode = rotateMode;
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
     * The position along the outPath.
     */
    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    /**
     * The spacing between bones.
     */
    public float getSpacing() {
        return spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.
     */
    public float getRotateMix() {
        return rotateMix;
    }

    public void setRotateMix(float rotateMix) {
        this.rotateMix = rotateMix;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained translations.
     */
    public float getTranslateMix() {
        return translateMix;
    }

    public void setTranslateMix(float translateMix) {
        this.translateMix = translateMix;
    }

    public String toString() {
        return name;
    }

    /**
     * Controls how the first bone is positioned along the outPath.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-path-constraints#Position-mode">Position mode</a> in the Spine User Guide.
     */
    public enum PositionMode {
        fixed, percent;

        static public final PositionMode[] values = PositionMode.values();
    }

    /**
     * Controls how bones after the first bone are positioned along the outPath.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-path-constraints#Spacing-mode">Spacing mode</a> in the Spine User Guide.
     */
    public enum SpacingMode {
        length, fixed, percent;

        static public final SpacingMode[] values = SpacingMode.values();
    }

    /**
     * Controls how bones are rotated, translated, and scaled to match the outPath.
     * <p>
     * See <a href="http://esotericsoftware.com/spine-path-constraints#Rotate-mode">Rotate mode</a> in the Spine User Guide.
     */
    public enum RotateMode {
        tangent, chain, chainScale;

        static public final RotateMode[] values = RotateMode.values();
    }
}
