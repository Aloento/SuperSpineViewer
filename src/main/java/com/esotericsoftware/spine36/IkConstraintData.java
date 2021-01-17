package com.esotericsoftware.spine36;

import com.badlogic.gdx.utils.Array;

public class IkConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    int order;
    BoneData target;
    int bendDirection = 1;
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

    
    public int getBendDirection() {
        return bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    
    public float getMix() {
        return mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public String toString() {
        return name;
    }
}
