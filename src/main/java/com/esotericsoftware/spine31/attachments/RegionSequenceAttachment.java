package com.esotericsoftware.spine31.attachments;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine31.Slot;

/**
 * Attachment that displays various texture regions over time.
 */
public class RegionSequenceAttachment extends RegionAttachment {
    private Mode mode;
    private float frameTime;
    private TextureRegion[] regions;

    public RegionSequenceAttachment(String name) {
        super(name);
    }

    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        if (regions == null) throw new IllegalStateException("Regions have not been set: " + this);

        int frameIndex = (int) (slot.getAttachmentTime() / frameTime);
        switch (mode) {
            case forward -> frameIndex = Math.min(regions.length - 1, frameIndex);
            case forwardLoop -> frameIndex = frameIndex % regions.length;
            case pingPong -> {
                frameIndex = frameIndex % (regions.length * 2);
                if (frameIndex >= regions.length) frameIndex = regions.length - 1 - (frameIndex - regions.length);
            }
            case random -> frameIndex = MathUtils.random(regions.length - 1);
            case backward -> frameIndex = Math.max(regions.length - frameIndex - 1, 0);
            case backwardLoop -> {
                frameIndex = frameIndex % regions.length;
                frameIndex = regions.length - frameIndex - 1;
            }
        }
        setRegion(regions[frameIndex]);

        return super.updateWorldVertices(slot, premultipliedAlpha);
    }

    public TextureRegion[] getRegions() {
        if (regions == null) throw new IllegalStateException("Regions have not been set: " + this);
        return regions;
    }

    public void setRegions(TextureRegion[] regions) {
        this.regions = regions;
    }

    /**
     * Sets the time in seconds each frame is shown.
     */
    public void setFrameTime(float frameTime) {
        this.frameTime = frameTime;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public enum Mode {
        forward, backward, forwardLoop, backwardLoop, pingPong, random
    }
}
