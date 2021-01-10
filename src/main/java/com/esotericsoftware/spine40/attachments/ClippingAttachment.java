package com.esotericsoftware.spine40.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.spine40.SlotData;

/**
 * An attachment with vertices that make up a polygon used for clipping the rendering of other attachments.
 */
public class ClippingAttachment extends VertexAttachment {
    // Nonessential.
    final Color color = new Color(0.2275f, 0.2275f, 0.8078f, 1); // ce3a3aff
    @Null
    SlotData endSlot;

    public ClippingAttachment(String name) {
        super(name);
    }

    /**
     * Clipping is performed between the clipping attachment's slot and the end slot. If null clipping is done until the end of
     * the skeleton's rendering.
     */
    public @Null
    SlotData getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(@Null SlotData endSlot) {
        this.endSlot = endSlot;
    }

    /**
     * The color of the clipping attachment as it was in Spine, or a default color if nonessential data was not exported. Clipping
     * attachments are not usually rendered at runtime.
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
