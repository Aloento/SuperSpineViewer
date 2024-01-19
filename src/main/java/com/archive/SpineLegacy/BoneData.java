package com.archive.SpineLegacy;

import com.badlogic.gdx.graphics.Color;

public class BoneData {
    final BoneData parent;
    final String name;
    final Color color = new Color(0.61f, 0.61f, 0.61f, 1);
    float length;
    float x, y, rotation, scaleX = 1, scaleY = 1, shearX, shearY;
    boolean inheritScale = true, inheritRotation = true;

    public BoneData(String name, BoneData parent) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
        this.parent = parent;
    }

    // public BoneData(BoneData bone, BoneData parent) {
    //     if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
    //     this.parent = parent;
    //     name = bone.name;
    //     length = bone.length;
    //     x = bone.x;
    //     y = bone.y;
    //     rotation = bone.rotation;
    //     scaleX = bone.scaleX;
    //     scaleY = bone.scaleY;
    //     shearX = bone.shearX;
    //     shearY = bone.shearY;
    // }

    // public BoneData getParent() {
    //     return parent;
    // }

    // public String getName() {
    //     return name;
    // }

    // public float getLength() {
    //     return length;
    // }

    // public void setLength(float length) {
    //     this.length = length;
    // }

    // public float getX() {
    //     return x;
    // }

    // public void setX(float x) {
    //     this.x = x;
    // }

    // public float getY() {
    //     return y;
    // }

    // public void setY(float y) {
    //     this.y = y;
    // }

    // public void setPosition(float x, float y) {
    //     this.x = x;
    //     this.y = y;
    // }

    // public float getRotation() {
    //     return rotation;
    // }

    // public void setRotation(float rotation) {
    //     this.rotation = rotation;
    // }

    // public float getScaleX() {
    //     return scaleX;
    // }

    // public void setScaleX(float scaleX) {
    //     this.scaleX = scaleX;
    // }

    // public float getScaleY() {
    //     return scaleY;
    // }

    // public void setScaleY(float scaleY) {
    //     this.scaleY = scaleY;
    // }

    // public void setScale(float scaleX, float scaleY) {
    //     this.scaleX = scaleX;
    //     this.scaleY = scaleY;
    // }

    // public float getShearX() {
    //     return shearX;
    // }

    // public void setShearX(float shearX) {
    //     this.shearX = shearX;
    // }

    // public float getShearY() {
    //     return shearY;
    // }

    // public void setShearY(float shearY) {
    //     this.shearY = shearY;
    // }

    // public boolean getInheritScale() {
    //     return inheritScale;
    // }

    // public void setInheritScale(boolean inheritScale) {
    //     this.inheritScale = inheritScale;
    // }

    // public boolean getInheritRotation() {
    //     return inheritRotation;
    // }

    // public void setInheritRotation(boolean inheritRotation) {
    //     this.inheritRotation = inheritRotation;
    // }

    public Color getColor() {
        return color;
    }

    public String toString() {
        return name;
    }
}
