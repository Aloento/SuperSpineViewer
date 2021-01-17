package com.esotericsoftware.spine37.attachments;

import com.badlogic.gdx.graphics.Color;

public class PathAttachment extends VertexAttachment {

    final Color color = new Color(1, 0.5f, 0, 1);
    float[] lengths;
    boolean closed, constantSpeed;

    public PathAttachment(String name) {
        super(name);
    }

    public boolean getClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public boolean getConstantSpeed() {
        return constantSpeed;
    }

    public void setConstantSpeed(boolean constantSpeed) {
        this.constantSpeed = constantSpeed;
    }

    public float[] getLengths() {
        return lengths;
    }

    public void setLengths(float[] lengths) {
        this.lengths = lengths;
    }
    
    public Color getColor() {
        return color;
    }
}
