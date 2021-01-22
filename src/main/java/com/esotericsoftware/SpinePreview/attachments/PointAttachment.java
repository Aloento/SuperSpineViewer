package com.esotericsoftware.SpinePreview.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.SpinePreview.Bone;

import static com.badlogic.gdx.math.MathUtils.*;

public class PointAttachment extends Attachment {
    final Color color = new Color(0.9451f, 0.9451f, 0, 1);
    float x, y, rotation;

    public PointAttachment(String name) {
        super(name);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public Color getColor() {
        return color;
    }

    public Vector2 computeWorldPosition(Bone bone, Vector2 point) {
        point.x = x * bone.getA() + y * bone.getB() + bone.getWorldX();
        point.y = x * bone.getC() + y * bone.getD() + bone.getWorldY();
        return point;
    }

    public float computeWorldRotation(Bone bone) {
        float cos = cosDeg(rotation), sin = sinDeg(rotation);
        float x = cos * bone.getA() + sin * bone.getB();
        float y = cos * bone.getC() + sin * bone.getD();
        return (float) Math.atan2(y, x) * radDeg;
    }

    public Attachment copy() {
        PointAttachment copy = new PointAttachment(name);
        copy.x = x;
        copy.y = y;
        copy.rotation = rotation;
        copy.color.set(color);
        return copy;
    }
}
