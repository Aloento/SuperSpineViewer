package com.archive.SpineStandard.attachments;

import com.badlogic.gdx.graphics.Color;

public class PointAttachment extends Attachment {
    final Color color = new Color(0.9451f, 0.9451f, 0, 1);
    float x, y, rotation;

    public PointAttachment(String name) {
        super(name);
    }

    // public float getX() {
    //     return x;
    // }

    public void setX(float x) {
        this.x = x;
    }

    // public float getY() {
    //     return y;
    // }

    public void setY(float y) {
        this.y = y;
    }

    // public float getRotation() {
    //     return rotation;
    // }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public Color getColor() {
        return color;
    }

    // public Attachment copy() {
    //     PointAttachment copy = new PointAttachment(name);
    //     copy.x = x;
    //     copy.y = y;
    //     copy.rotation = rotation;
    //     copy.color.set(color);
    //     return copy;
    // }
}
