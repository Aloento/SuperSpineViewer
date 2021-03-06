package com.esotericsoftware.SpineStandard;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.SpineStandard.attachments.Attachment;

public class Slot {
    final SlotData data;
    final Bone bone;
    final Color color = new Color();
    private final FloatArray deform = new FloatArray();
    Color darkColor;
    Attachment attachment;
    int attachmentState;
    private float attachmentTime;

    public Slot(SlotData data, Bone bone) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
        this.data = data;
        this.bone = bone;
        darkColor = data.darkColor == null ? null : new Color();
        setToSetupPose();
    }

    // public Slot(Slot slot, Bone bone) {
    //     if (slot == null) throw new IllegalArgumentException("slot cannot be null.");
    //     if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
    //     data = slot.data;
    //     this.bone = bone;
    //     color.set(slot.color);
    //     attachment = slot.attachment;
    //     attachmentTime = slot.attachmentTime;
    //     if (Loader.spineVersion > 37)
    //         deform.addAll(slot.deform);
    //     else if (Loader.spineVersion > 35)
    //         darkColor = slot.darkColor == null ? null : new Color(slot.darkColor);
    // }

    public SlotData getData() {
        return data;
    }

    public Bone getBone() {
        return bone;
    }

    public Skeleton getSkeleton() {
        return bone.skeleton;
    }

    public Color getColor() {
        return color;
    }

    public Color getDarkColor() {
        return darkColor;
    }

    // public Attachment getAttachment() {
    //     return attachment;
    // }

    public void setAttachment(Attachment attachment) {
        if (this.attachment == attachment) return;
        this.attachment = attachment;
        attachmentTime = bone.skeleton.time;
        deform.clear();
    }

    // public float getAttachmentTime() {
    //     return bone.skeleton.time - attachmentTime;
    // }

    // public void setAttachmentTime(float time) {
    //     attachmentTime = bone.skeleton.time - time;
    // }

    public FloatArray getAttachmentVertices() {
        return deform;
    }

    // public void setAttachmentVertices(FloatArray attachmentVertices) {
    //     this.setDeform(attachmentVertices);
    // }

    public FloatArray getDeform() {
        return deform;
    }

    // public void setDeform(FloatArray deform) {
    //     if (deform == null) throw new IllegalArgumentException("deform(attachmentVertices) cannot be null.");
    //     this.deform = deform;
    // }

    public void setToSetupPose() {
        color.set(data.color);
        if (darkColor != null) darkColor.set(data.darkColor);
        if (data.attachmentName == null)
            setAttachment(null);
        else {
            attachment = null;
            setAttachment(bone.skeleton.getAttachment(data.index, data.attachmentName));
        }
    }

    public String toString() {
        return data.name;
    }
}
