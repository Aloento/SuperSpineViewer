package com.esotericsoftware.spine21;

import com.badlogic.gdx.graphics.Color;

public class SlotData {
    final String name;
    final BoneData boneData;
    final Color color = new Color(1, 1, 1, 1);
    String attachmentName;
    boolean additiveBlending;

    SlotData() {
        name = null;
        boneData = null;
    }

    public SlotData(String name, BoneData boneData) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        if (boneData == null) throw new IllegalArgumentException("boneData cannot be null.");
        this.name = name;
        this.boneData = boneData;
    }

    public String getName() {
        return name;
    }

    public BoneData getBoneData() {
        return boneData;
    }

    public Color getColor() {
        return color;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public boolean getAdditiveBlending() {
        return additiveBlending;
    }

    public void setAdditiveBlending(boolean additiveBlending) {
        this.additiveBlending = additiveBlending;
    }

    public String toString() {
        return name;
    }
}
