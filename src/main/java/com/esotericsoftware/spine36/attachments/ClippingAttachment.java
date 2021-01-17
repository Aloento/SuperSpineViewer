package com.esotericsoftware.spine36.attachments;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine36.SlotData;


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
}
