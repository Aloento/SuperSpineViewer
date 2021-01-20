package com.esotericsoftware.SpinePreview.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.SpinePreview.SlotData;


public class ClippingAttachment extends VertexAttachment {

    final Color color = new Color(0.2275f, 0.2275f, 0.8078f, 1);
    @Null
    SlotData endSlot;

    public ClippingAttachment(String name) {
        super(name);
    }


    public @Null
    SlotData getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(@Null SlotData endSlot) {
        this.endSlot = endSlot;
    }


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
