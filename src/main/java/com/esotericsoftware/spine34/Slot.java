package com.esotericsoftware.spine34;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine34.attachments.Attachment;

public class Slot {
    final SlotData data;
    final Bone bone;
    final Color color;
    Attachment attachment;
    private float attachmentTime;
    private FloatArray attachmentVertices = new FloatArray();

    public Slot(SlotData data, Bone bone) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
        this.data = data;
        this.bone = bone;
        color = new Color();
        setToSetupPose();
    }

    
    public Slot(Slot slot, Bone bone) {
        if (slot == null) throw new IllegalArgumentException("slot cannot be null.");
        if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
        data = slot.data;
        this.bone = bone;
        color = new Color(slot.color);
        attachment = slot.attachment;
        attachmentTime = slot.attachmentTime;
    }

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

    
    public Attachment getAttachment() {
        return attachment;
    }

    
    public void setAttachment(Attachment attachment) {
        if (this.attachment == attachment) return;
        this.attachment = attachment;
        attachmentTime = bone.skeleton.time;
        attachmentVertices.clear();
    }

    
    public float getAttachmentTime() {
        return bone.skeleton.time - attachmentTime;
    }

    public void setAttachmentTime(float time) {
        attachmentTime = bone.skeleton.time - time;
    }

    public FloatArray getAttachmentVertices() {
        return attachmentVertices;
    }

    public void setAttachmentVertices(FloatArray attachmentVertices) {
        if (attachmentVertices == null) throw new IllegalArgumentException("attachmentVertices cannot be null.");
        this.attachmentVertices = attachmentVertices;
    }

    public void setToSetupPose() {
        color.set(data.color);
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
