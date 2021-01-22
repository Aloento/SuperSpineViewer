package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.Array;

public class IkConstraintData extends ConstraintData {
    final Array<BoneData> bones = new Array();
    BoneData target;
    int bendDirection = 1;
    boolean compress, stretch, uniform;
    float mix = 1, softness;

    public IkConstraintData(String name) {
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

    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public float getSoftness() {
        return softness;
    }

    public void setSoftness(float softness) {
        this.softness = softness;
    }

    public int getBendDirection() {
        return bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    public boolean getCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public boolean getStretch() {
        return stretch;
    }

    public void setStretch(boolean stretch) {
        this.stretch = stretch;
    }

    public boolean getUniform() {
        return uniform;
    }

    public void setUniform(boolean uniform) {
        this.uniform = uniform;
    }
}
