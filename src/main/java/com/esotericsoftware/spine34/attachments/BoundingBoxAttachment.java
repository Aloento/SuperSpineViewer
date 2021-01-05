package com.esotericsoftware.spine34.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine34.Slot;

public class BoundingBoxAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(0.38f, 0.94f, 0, 1);

    public BoundingBoxAttachment(String name) {
        super(name);
    }

    public void computeWorldVertices(Slot slot, float[] worldVertices) {
        computeWorldVertices(slot, 0, worldVerticesLength, worldVertices, 0);
    }

    public Color getColor() {
        return color;
    }
}
