package com.esotericsoftware.Spine40;

import com.badlogic.gdx.utils.Array;

/**
 * Stores the setup pose for an {@link IkConstraint}.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-ik-constraints">IK constraints</a> in the Spine User Guide.
 */
public class IkConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array();
    BoneData target;
    int bendDirection = 1;
    boolean compress, stretch, uniform;
    float mix = 1, softness;

    public IkConstraintData(String name) {
        super(name);
    }

    /**
     * The bones that are constrained by this IK constraint.
     */
    public Array<BoneData> getBones() {
        return bones;
    }

    /**
     * The bone that is the IK target.
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
     * <p>
     * For two bone IK: if the parent bone has local nonuniform scale, the child bone's local Y translation is set to 0.
     */
    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    /**
     * For two bone IK, the target bone's distance from the maximum reach of the bones where rotation begins to slow. The bones
     * will not straighten completely until the target is this far out of range.
     */
    public float getSoftness() {
        return softness;
    }

    public void setSoftness(float softness) {
        this.softness = softness;
    }

    /**
     * For two bone IK, controls the bend direction of the IK bones, either 1 or -1.
     */
    public int getBendDirection() {
        return bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    /**
     * For one bone IK, when true and the target is too close, the bone is scaled to reach it.
     */
    public boolean getCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    /**
     * When true and the target is out of range, the parent bone is scaled to reach it.
     * <p>
     * For two bone IK: 1) the child bone's local Y translation is set to 0, 2) stretch is not applied if {@link #getSoftness()} is
     * > 0, and 3) if the parent bone has local nonuniform scale, stretch is not applied.
     */
    public boolean getStretch() {
        return stretch;
    }

    public void setStretch(boolean stretch) {
        this.stretch = stretch;
    }

    /**
     * When true and {@link #getCompress()} or {@link #getStretch()} is used, the bone is scaled on both the X and Y axes.
     */
    public boolean getUniform() {
        return uniform;
    }

    public void setUniform(boolean uniform) {
        this.uniform = uniform;
    }
}
