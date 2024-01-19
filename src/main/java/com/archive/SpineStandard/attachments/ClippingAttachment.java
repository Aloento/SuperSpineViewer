package com.archive.SpineStandard.attachments;

import com.badlogic.gdx.graphics.Color;
import com.archive.SpineStandard.SlotData;

public class ClippingAttachment extends VertexAttachment {
    final Color color = new Color(0.2275f, 0.2275f, 0.8078f, 1);
    SlotData endSlot;

    public ClippingAttachment(String name) {
        super(name);
    }

    public SlotData getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(SlotData endSlot) {
        this.endSlot = endSlot;
    }

    public Color getColor() {
        return color;
    }

    // public Attachment copy() {
    //     ClippingAttachment copy = new ClippingAttachment(name);
    //     copyTo(copy);
    //     copy.endSlot = endSlot;
    //     copy.color.set(color);
    //     return copy;
    // }
}
