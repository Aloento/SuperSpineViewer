package com.esotericsoftware.Spine40;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;

/**
 * Stores the setup pose for a {@link Slot}.
 */
public class SlotData {
    final int index;
    final String name;
    final BoneData boneData;
    final Color color = new Color(1, 1, 1, 1);
    @Null
    Color darkColor;
    @Null
    String attachmentName;
    BlendMode blendMode;

    public SlotData(int index, String name, BoneData boneData) {
        if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        if (boneData == null) throw new IllegalArgumentException("boneData cannot be null.");
        this.index = index;
        this.name = name;
        this.boneData = boneData;
    }

    /**
     * The index of the slot in {@link Skeleton#getSlots()}.
     */
    public int getIndex() {
        return index;
    }

    /**
     * The name of the slot, which is unique across all slots in the skeleton.
     */
    public String getName() {
        return name;
    }

    /**
     * The bone this slot belongs to.
     */
    public BoneData getBoneData() {
        return boneData;
    }

    /**
     * The color used to tint the slot's attachment. If {@link #getDarkColor()} is set, this is used as the light color for two
     * color tinting.
     */
    public Color getColor() {
        return color;
    }

    /**
     * The dark color used to tint the slot's attachment for two color tinting, or null if two color tinting is not used. The dark
     * color's alpha is not used.
     */
    public @Null Color getDarkColor() {
        return darkColor;
    }

    public void setDarkColor(@Null Color darkColor) {
        this.darkColor = darkColor;
    }

    /**
     * The name of the attachment that is visible for this slot in the setup pose, or null if no attachment is visible.
     */
    public @Null String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(@Null String attachmentName) {
        this.attachmentName = attachmentName;
    }

    /**
     * The blend mode for drawing the slot's attachment.
     */
    public BlendMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(BlendMode blendMode) {
        if (blendMode == null) throw new IllegalArgumentException("blendMode cannot be null.");
        this.blendMode = blendMode;
    }

    public String toString() {
        return name;
    }
}
