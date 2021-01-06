package com.esotericsoftware.spine38.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine38.SlotData;

/**
 * An attachment with vertices that make up a polygon used for clipping the rendering of other attachments.
 */
public class ClippingAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(0.2275f, 0.2275f, 0.8078f, 1); // ce3a3aff
    SlotData endSlot;

    public ClippingAttachment(String name) {
        super(name);
    }

    /**
     * Clipping is performed between the clipping polygon's slot and the end slot. Returns -1 if clipping is done until the end of
     * the skeleton's rendering.
     */
    public SlotData getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(SlotData endSlot) {
        this.endSlot = endSlot;
    }

    /**
     * The color of the clipping polygon as it was in Spine. Available only when nonessential data was exported. Clipping polygons
     * are not usually rendered at runtime.
     */
    public Color getColor() {
        return color;
    }

    public Attachment copy() {
        ClippingAttachment copy = new ClippingAttachment(name);
        copyTo(copy);
        copy.endSlot = endSlot;
        copy.color.set(color);
        return copy;
    }
}
