package com.esotericsoftware.spine34.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine34.Slot;

public class PathAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(1, 0.5f, 0, 1);
    float[] lengths;
    boolean closed, constantSpeed;

    public PathAttachment(String name) {
        super(name);
    }

    public void computeWorldVertices(Slot slot, float[] worldVertices) {
        super.computeWorldVertices(slot, worldVertices);
    }

    public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset) {
        super.computeWorldVertices(slot, start, count, worldVertices, offset);
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

    /**
     * Returns the length in the setup pose from the start of the outPath to the end of each curve.
     */
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
