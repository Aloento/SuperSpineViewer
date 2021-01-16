package com.esotericsoftware.spine37;

import com.badlogic.gdx.utils.Array;


public class IkConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    int order;
    BoneData target;
    int bendDirection = 1;
    boolean compress, stretch, uniform;
    float mix = 1;

    public IkConstraintData(String name) {
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

    
    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
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

    public String toString() {
        return name;
    }
}
