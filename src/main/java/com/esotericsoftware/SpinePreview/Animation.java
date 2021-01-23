package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectSet;
import com.esotericsoftware.SpinePreview.attachments.Attachment;
import com.esotericsoftware.SpinePreview.attachments.VertexAttachment;

import static com.esotericsoftware.SpinePreview.Animation.MixBlend.*;
import static com.esotericsoftware.SpinePreview.Animation.MixDirection.in;
import static com.esotericsoftware.SpinePreview.Animation.MixDirection.out;
import static com.esotericsoftware.SpinePreview.utils.SpineUtils.arraycopy;

public class Animation {
    final String name;
    final ObjectSet<String> timelineIds = new ObjectSet<>();
    Array<Timeline> timelines;
    float duration;

    public Animation(String name, Array<Timeline> timelines, float duration) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
        this.duration = duration;
        setTimelines(timelines);
    }

    static int search(float[] frames, float time) {
        int n = frames.length;
        for (int i = 1; i < n; i++)
            if (frames[i] > time) return i - 1;
        return n - 1;
    }

    static int search(float[] frames, float time, int step) {
        int n = frames.length;
        for (int i = step; i < n; i += step)
            if (frames[i] > time) return i - step;
        return n - step;
    }

    // public Array<Timeline> getTimelines() {
    //     return timelines;
    // }

    public void setTimelines(Array<Timeline> timelines) {
        if (timelines == null) throw new IllegalArgumentException("timelines cannot be null.");
        this.timelines = timelines;
        timelineIds.clear();
        Object[] items = timelines.items;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelineIds.addAll(((Timeline) items[i]).getPropertyIds());
    }

    public boolean hasTimeline(String[] propertyIds) {
        for (String id : propertyIds)
            if (timelineIds.contains(id)) return true;
        return false;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, @Null Array<Event> events, float alpha,
                      MixBlend blend, MixDirection direction) {
        // if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        // if (loop && duration != 0) {
        //     time %= duration;
        //     if (lastTime > 0) lastTime %= duration;
        // }
        // Object[] timelines = this.timelines.items;
        // for (int i = 0, n = this.timelines.size; i < n; i++)
        //     ((Timeline) timelines[i]).apply(skeleton, lastTime, time, events, alpha, blend, direction);
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public enum MixBlend {
        setup,
        first,
        replace,
        add
    }

    public enum MixDirection {
        in, out
    }

    private enum Property {
        rotate, x, y, scaleX, scaleY, shearX, shearY,
        rgb, alpha, rgb2,
        attachment, deform,
        event, drawOrder,
        ikConstraint, transformConstraint,
        pathConstraintPosition, pathConstraintSpacing, pathConstraintMix
    }

    public interface BoneTimeline {
        int getBoneIndex();
    }

    public interface SlotTimeline {
        int getSlotIndex();
    }

    static public abstract class Timeline {
        final float[] frames;
        private final String[] propertyIds;

        public Timeline(int frameCount, String... propertyIds) {
            if (propertyIds == null) throw new IllegalArgumentException("propertyIds cannot be null.");
            this.propertyIds = propertyIds;
            frames = new float[frameCount * getFrameEntries()];
        }

        public String[] getPropertyIds() {
            return propertyIds;
        }

        // public float[] getFrames() {
        //     return frames;
        // }

        public int getFrameEntries() {
            return 1;
        }

        public int getFrameCount() {
            return frames.length / getFrameEntries();
        }

        public float getDuration() {
            return frames[frames.length - getFrameEntries()];
        }

        abstract public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha,
                                   MixBlend blend, MixDirection direction);
    }

    static public abstract class CurveTimeline extends Timeline {
        static public final int LINEAR = 0, STEPPED = 1, BEZIER = 2, BEZIER_SIZE = 18;
        float[] curves;

        public CurveTimeline(int frameCount, int bezierCount, String... propertyIds) {
            super(frameCount, propertyIds);
            curves = new float[frameCount + bezierCount * BEZIER_SIZE];
            curves[frameCount - 1] = STEPPED;
        }

        // public void setLinear(int frame) {
        //     curves[frame] = LINEAR;
        // }

        public void setStepped(int frame) {
            curves[frame] = STEPPED;
        }

        // public int getCurveType(int frame) {
        //     return (int) curves[frame];
        // }

        public void shrink(int bezierCount) {
            int size = getFrameCount() + bezierCount * BEZIER_SIZE;
            if (curves.length > size) {
                float[] newCurves = new float[size];
                arraycopy(curves, 0, newCurves, 0, size);
                curves = newCurves;
            }
        }

        public void setBezier(int bezier, int frame, int value, float time1, float value1, float cx1, float cy1, float cx2,
                              float cy2, float time2, float value2) {
            float[] curves = this.curves;
            int i = getFrameCount() + bezier * BEZIER_SIZE;
            if (value == 0) curves[frame] = BEZIER + i;
            float tmpx = (time1 - cx1 * 2 + cx2) * 0.03f, tmpy = (value1 - cy1 * 2 + cy2) * 0.03f;
            float dddx = ((cx1 - cx2) * 3 - time1 + time2) * 0.006f, dddy = ((cy1 - cy2) * 3 - value1 + value2) * 0.006f;
            float ddx = tmpx * 2 + dddx, ddy = tmpy * 2 + dddy;
            float dx = (cx1 - time1) * 0.3f + tmpx + dddx * 0.16666667f, dy = (cy1 - value1) * 0.3f + tmpy + dddy * 0.16666667f;
            float x = time1 + dx, y = value1 + dy;
            for (int n = i + BEZIER_SIZE; i < n; i += 2) {
                curves[i] = x;
                curves[i + 1] = y;
                dx += ddx;
                dy += ddy;
                ddx += dddx;
                ddy += dddy;
                x += dx;
                y += dy;
            }
        }

        public float getBezierValue(float time, int frameIndex, int valueOffset, int i) {
            float[] curves = this.curves;
            if (curves[i] > time) {
                float x = frames[frameIndex], y = frames[frameIndex + valueOffset];
                return y + (time - x) / (curves[i] - x) * (curves[i + 1] - y);
            }
            int n = i + BEZIER_SIZE;
            for (i += 2; i < n; i += 2) {
                if (curves[i] >= time) {
                    float x = curves[i - 2], y = curves[i - 1];
                    return y + (time - x) / (curves[i] - x) * (curves[i + 1] - y);
                }
            }
            frameIndex += getFrameEntries();
            float x = curves[n - 2], y = curves[n - 1];
            return y + (time - x) / (frames[frameIndex] - x) * (frames[frameIndex + valueOffset] - y);
        }
    }

    static public abstract class CurveTimeline1 extends CurveTimeline {
        static public final int ENTRIES = 2;
        static final int VALUE = 1;

        public CurveTimeline1(int frameCount, int bezierCount, String... propertyIds) {
            super(frameCount, bezierCount, propertyIds);
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public void setFrame(int frame, float time, float value) {
            frame <<= 1;
            frames[frame] = time;
            frames[frame + VALUE] = value;
        }

        public float getCurveValue(float time) {
            float[] frames = this.frames;
            int i = frames.length - 2;
            for (int ii = 2; ii <= i; ii += 2) {
                if (frames[ii] > time) {
                    i = ii - 2;
                    break;
                }
            }
            int curveType = (int) curves[i >> 1];
            switch (curveType) {
                case LINEAR:
                    float before = frames[i], value = frames[i + VALUE];
                    return value + (time - before) / (frames[i + ENTRIES] - before) * (frames[i + ENTRIES + VALUE] - value);
                case STEPPED:
                    return frames[i + VALUE];
            }
            return getBezierValue(time, i, VALUE, curveType - BEZIER);
        }
    }

    static public abstract class CurveTimeline2 extends CurveTimeline {
        static public final int ENTRIES = 3;
        static final int VALUE1 = 1, VALUE2 = 2;

        public CurveTimeline2(int frameCount, int bezierCount, String... propertyIds) {
            super(frameCount, bezierCount, propertyIds);
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public void setFrame(int frame, float time, float value1, float value2) {
            frame *= ENTRIES;
            frames[frame] = time;
            frames[frame + VALUE1] = value1;
            frames[frame + VALUE2] = value2;
        }
    }

    static public class RotateTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public RotateTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.rotate.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.rotation = bone.data.rotation;
                        return;
                    }
                    case first -> bone.rotation += (bone.data.rotation - bone.rotation) * alpha;
                }
                return;
            }
            float r = getCurveValue(time);
            switch (blend) {
                case setup:
                    bone.rotation = bone.data.rotation + r * alpha;
                    break;
                case first:
                case replace:
                    r += bone.data.rotation - bone.rotation;
                case add:
                    bone.rotation += r * alpha;
            }
        }
    }

    static public class TranslateTimeline extends CurveTimeline2 implements BoneTimeline {
        final int boneIndex;

        public TranslateTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount,
                    Property.x.ordinal() + "|" + boneIndex,
                    Property.y.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.x = bone.data.x;
                        bone.y = bone.data.y;
                        return;
                    }
                    case first -> {
                        bone.x += (bone.data.x - bone.x) * alpha;
                        bone.y += (bone.data.y - bone.y) * alpha;
                    }
                }
                return;
            }
            float x, y;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    x += (frames[i + ENTRIES + VALUE1] - x) * t;
                    y += (frames[i + ENTRIES + VALUE2] - y) * t;
                }
                case STEPPED -> {
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                }
                default -> {
                    x = getBezierValue(time, i, VALUE1, curveType - BEZIER);
                    y = getBezierValue(time, i, VALUE2, curveType + BEZIER_SIZE - BEZIER);
                }
            }
            switch (blend) {
                case setup -> {
                    bone.x = bone.data.x + x * alpha;
                    bone.y = bone.data.y + y * alpha;
                }
                case first, replace -> {
                    bone.x += (bone.data.x + x - bone.x) * alpha;
                    bone.y += (bone.data.y + y - bone.y) * alpha;
                }
                case add -> {
                    bone.x += x * alpha;
                    bone.y += y * alpha;
                }
            }
        }
    }

    static public class TranslateXTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public TranslateXTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.x.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.x = bone.data.x;
                        return;
                    }
                    case first -> bone.x += (bone.data.x - bone.x) * alpha;
                }
                return;
            }
            float x = getCurveValue(time);
            switch (blend) {
                case setup -> bone.x = bone.data.x + x * alpha;
                case first, replace -> bone.x += (bone.data.x + x - bone.x) * alpha;
                case add -> bone.x += x * alpha;
            }
        }
    }

    static public class TranslateYTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public TranslateYTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.y.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.y = bone.data.y;
                        return;
                    }
                    case first -> bone.y += (bone.data.y - bone.y) * alpha;
                }
                return;
            }
            float y = getCurveValue(time);
            switch (blend) {
                case setup -> bone.y = bone.data.y + y * alpha;
                case first, replace -> bone.y += (bone.data.y + y - bone.y) * alpha;
                case add -> bone.y += y * alpha;
            }
        }
    }

    static public class ScaleTimeline extends CurveTimeline2 implements BoneTimeline {
        final int boneIndex;

        public ScaleTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount,
                    Property.scaleX.ordinal() + "|" + boneIndex,
                    Property.scaleY.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.scaleX = bone.data.scaleX;
                        bone.scaleY = bone.data.scaleY;
                        return;
                    }
                    case first -> {
                        bone.scaleX += (bone.data.scaleX - bone.scaleX) * alpha;
                        bone.scaleY += (bone.data.scaleY - bone.scaleY) * alpha;
                    }
                }
                return;
            }
            float x, y;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    x += (frames[i + ENTRIES + VALUE1] - x) * t;
                    y += (frames[i + ENTRIES + VALUE2] - y) * t;
                }
                case STEPPED -> {
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                }
                default -> {
                    x = getBezierValue(time, i, VALUE1, curveType - BEZIER);
                    y = getBezierValue(time, i, VALUE2, curveType + BEZIER_SIZE - BEZIER);
                }
            }
            x *= bone.data.scaleX;
            y *= bone.data.scaleY;
            if (alpha == 1) {
                if (blend == add) {
                    bone.scaleX += x - bone.data.scaleX;
                    bone.scaleY += y - bone.data.scaleY;
                } else {
                    bone.scaleX = x;
                    bone.scaleY = y;
                }
            } else {
                float bx, by;
                if (direction == out) {
                    switch (blend) {
                        case setup -> {
                            bx = bone.data.scaleX;
                            by = bone.data.scaleY;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - by) * alpha;
                        }
                        case first, replace -> {
                            bx = bone.scaleX;
                            by = bone.scaleY;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - by) * alpha;
                        }
                        case add -> {
                            bx = bone.scaleX;
                            by = bone.scaleY;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bone.data.scaleX) * alpha;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - bone.data.scaleY) * alpha;
                        }
                    }
                } else {
                    switch (blend) {
                        case setup -> {
                            bx = Math.abs(bone.data.scaleX) * Math.signum(x);
                            by = Math.abs(bone.data.scaleY) * Math.signum(y);
                            bone.scaleX = bx + (x - bx) * alpha;
                            bone.scaleY = by + (y - by) * alpha;
                        }
                        case first, replace -> {
                            bx = Math.abs(bone.scaleX) * Math.signum(x);
                            by = Math.abs(bone.scaleY) * Math.signum(y);
                            bone.scaleX = bx + (x - bx) * alpha;
                            bone.scaleY = by + (y - by) * alpha;
                        }
                        case add -> {
                            bx = Math.signum(x);
                            by = Math.signum(y);
                            bone.scaleX = Math.abs(bone.scaleX) * bx + (x - Math.abs(bone.data.scaleX) * bx) * alpha;
                            bone.scaleY = Math.abs(bone.scaleY) * by + (y - Math.abs(bone.data.scaleY) * by) * alpha;
                        }
                    }
                }
            }
        }
    }

    static public class ScaleXTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public ScaleXTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.scaleX.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.scaleX = bone.data.scaleX;
                        return;
                    }
                    case first -> bone.scaleX += (bone.data.scaleX - bone.scaleX) * alpha;
                }
                return;
            }
            float x = getCurveValue(time) * bone.data.scaleX;
            if (alpha == 1) {
                if (blend == add)
                    bone.scaleX += x - bone.data.scaleX;
                else
                    bone.scaleX = x;
            } else {
                float bx;
                if (direction == out) {
                    switch (blend) {
                        case setup -> {
                            bx = bone.data.scaleX;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                        }
                        case first, replace -> {
                            bx = bone.scaleX;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                        }
                        case add -> {
                            bx = bone.scaleX;
                            bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bone.data.scaleX) * alpha;
                        }
                    }
                } else {
                    switch (blend) {
                        case setup -> {
                            bx = Math.abs(bone.data.scaleX) * Math.signum(x);
                            bone.scaleX = bx + (x - bx) * alpha;
                        }
                        case first, replace -> {
                            bx = Math.abs(bone.scaleX) * Math.signum(x);
                            bone.scaleX = bx + (x - bx) * alpha;
                        }
                        case add -> {
                            bx = Math.signum(x);
                            bone.scaleX = Math.abs(bone.scaleX) * bx + (x - Math.abs(bone.data.scaleX) * bx) * alpha;
                        }
                    }
                }
            }
        }
    }

    static public class ScaleYTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public ScaleYTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.scaleY.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.scaleY = bone.data.scaleY;
                        return;
                    }
                    case first -> bone.scaleY += (bone.data.scaleY - bone.scaleY) * alpha;
                }
                return;
            }
            float y = getCurveValue(time) * bone.data.scaleY;
            if (alpha == 1) {
                if (blend == add)
                    bone.scaleY += y - bone.data.scaleY;
                else
                    bone.scaleY = y;
            } else {
                float by;
                if (direction == out) {
                    switch (blend) {
                        case setup -> {
                            by = bone.data.scaleY;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - by) * alpha;
                        }
                        case first, replace -> {
                            by = bone.scaleY;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - by) * alpha;
                        }
                        case add -> {
                            by = bone.scaleY;
                            bone.scaleY = by + (Math.abs(y) * Math.signum(by) - bone.data.scaleY) * alpha;
                        }
                    }
                } else {
                    switch (blend) {
                        case setup -> {
                            by = Math.abs(bone.data.scaleY) * Math.signum(y);
                            bone.scaleY = by + (y - by) * alpha;
                        }
                        case first, replace -> {
                            by = Math.abs(bone.scaleY) * Math.signum(y);
                            bone.scaleY = by + (y - by) * alpha;
                        }
                        case add -> {
                            by = Math.signum(y);
                            bone.scaleY = Math.abs(bone.scaleY) * by + (y - Math.abs(bone.data.scaleY) * by) * alpha;
                        }
                    }
                }
            }
        }
    }

    static public class ShearTimeline extends CurveTimeline2 implements BoneTimeline {
        final int boneIndex;

        public ShearTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount,
                    Property.shearX.ordinal() + "|" + boneIndex,
                    Property.shearY.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.shearX = bone.data.shearX;
                        bone.shearY = bone.data.shearY;
                        return;
                    }
                    case first -> {
                        bone.shearX += (bone.data.shearX - bone.shearX) * alpha;
                        bone.shearY += (bone.data.shearY - bone.shearY) * alpha;
                    }
                }
                return;
            }
            float x, y;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    x += (frames[i + ENTRIES + VALUE1] - x) * t;
                    y += (frames[i + ENTRIES + VALUE2] - y) * t;
                }
                case STEPPED -> {
                    x = frames[i + VALUE1];
                    y = frames[i + VALUE2];
                }
                default -> {
                    x = getBezierValue(time, i, VALUE1, curveType - BEZIER);
                    y = getBezierValue(time, i, VALUE2, curveType + BEZIER_SIZE - BEZIER);
                }
            }
            switch (blend) {
                case setup -> {
                    bone.shearX = bone.data.shearX + x * alpha;
                    bone.shearY = bone.data.shearY + y * alpha;
                }
                case first, replace -> {
                    bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha;
                    bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha;
                }
                case add -> {
                    bone.shearX += x * alpha;
                    bone.shearY += y * alpha;
                }
            }
        }
    }

    static public class ShearXTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public ShearXTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.shearX.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.shearX = bone.data.shearX;
                        return;
                    }
                    case first -> bone.shearX += (bone.data.shearX - bone.shearX) * alpha;
                }
                return;
            }
            float x = getCurveValue(time);
            switch (blend) {
                case setup -> bone.shearX = bone.data.shearX + x * alpha;
                case first, replace -> bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha;
                case add -> bone.shearX += x * alpha;
            }
        }
    }

    static public class ShearYTimeline extends CurveTimeline1 implements BoneTimeline {
        final int boneIndex;

        public ShearYTimeline(int frameCount, int bezierCount, int boneIndex) {
            super(frameCount, bezierCount, Property.shearY.ordinal() + "|" + boneIndex);
            this.boneIndex = boneIndex;
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Bone bone = skeleton.bones.get(boneIndex);
            if (!bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.shearY = bone.data.shearY;
                        return;
                    }
                    case first -> bone.shearY += (bone.data.shearY - bone.shearY) * alpha;
                }
                return;
            }
            float y = getCurveValue(time);
            switch (blend) {
                case setup -> bone.shearY = bone.data.shearY + y * alpha;
                case first, replace -> bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha;
                case add -> bone.shearY += y * alpha;
            }
        }
    }

    static public class RGBATimeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 5;
        static private final int R = 1, G = 2, B = 3, A = 4;
        final int slotIndex;

        public RGBATimeline(int frameCount, int bezierCount, int slotIndex) {
            super(frameCount, bezierCount,
                    Property.rgb.ordinal() + "|" + slotIndex,
                    Property.alpha.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setFrame(int frame, float time, float r, float g, float b, float a) {
            frame *= ENTRIES;
            frames[frame] = time;
            frames[frame + R] = r;
            frames[frame + G] = g;
            frames[frame + B] = b;
            frames[frame + A] = a;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                Color color = slot.color, setup = slot.data.color;
                switch (blend) {
                    case setup -> {
                        color.set(setup);
                        return;
                    }
                    case first -> color.add((setup.r - color.r) * alpha, (setup.g - color.g) * alpha, (setup.b - color.b) * alpha,
                            (setup.a - color.a) * alpha);
                }
                return;
            }
            float r, g, b, a;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    a = frames[i + A];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    r += (frames[i + ENTRIES + R] - r) * t;
                    g += (frames[i + ENTRIES + G] - g) * t;
                    b += (frames[i + ENTRIES + B] - b) * t;
                    a += (frames[i + ENTRIES + A] - a) * t;
                }
                case STEPPED -> {
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    a = frames[i + A];
                }
                default -> {
                    r = getBezierValue(time, i, R, curveType - BEZIER);
                    g = getBezierValue(time, i, G, curveType + BEZIER_SIZE - BEZIER);
                    b = getBezierValue(time, i, B, curveType + BEZIER_SIZE * 2 - BEZIER);
                    a = getBezierValue(time, i, A, curveType + BEZIER_SIZE * 3 - BEZIER);
                }
            }
            Color color = slot.color;
            if (alpha == 1)
                slot.color.set(r, g, b, a);
            else {
                if (blend == setup) color.set(slot.data.color);
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            }
        }
    }

    static public class RGBTimeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 4;
        static private final int R = 1, G = 2, B = 3;
        final int slotIndex;

        public RGBTimeline(int frameCount, int bezierCount, int slotIndex) {
            super(frameCount, bezierCount, Property.rgb.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setFrame(int frame, float time, float r, float g, float b) {
            frame <<= 2;
            frames[frame] = time;
            frames[frame + R] = r;
            frames[frame + G] = g;
            frames[frame + B] = b;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                Color color = slot.color, setup = slot.data.color;
                switch (blend) {
                    case setup -> {
                        color.r = setup.r;
                        color.g = setup.g;
                        color.b = setup.b;
                        return;
                    }
                    case first -> {
                        color.r += (setup.r - color.r) * alpha;
                        color.g += (setup.g - color.g) * alpha;
                        color.b += (setup.b - color.b) * alpha;
                    }
                }
                return;
            }
            float r, g, b;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i >> 2];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    r += (frames[i + ENTRIES + R] - r) * t;
                    g += (frames[i + ENTRIES + G] - g) * t;
                    b += (frames[i + ENTRIES + B] - b) * t;
                }
                case STEPPED -> {
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                }
                default -> {
                    r = getBezierValue(time, i, R, curveType - BEZIER);
                    g = getBezierValue(time, i, G, curveType + BEZIER_SIZE - BEZIER);
                    b = getBezierValue(time, i, B, curveType + BEZIER_SIZE * 2 - BEZIER);
                }
            }
            Color color = slot.color;
            if (alpha == 1) {
                color.r = r;
                color.g = g;
                color.b = b;
            } else {
                if (blend == setup) {
                    Color setup = slot.data.color;
                    color.r = setup.r;
                    color.g = setup.g;
                    color.b = setup.b;
                }
                color.r += (r - color.r) * alpha;
                color.g += (g - color.g) * alpha;
                color.b += (b - color.b) * alpha;
            }
        }
    }

    static public class AlphaTimeline extends CurveTimeline1 implements SlotTimeline {
        final int slotIndex;

        public AlphaTimeline(int frameCount, int bezierCount, int slotIndex) {
            super(frameCount, bezierCount, Property.alpha.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                Color color = slot.color, setup = slot.data.color;
                switch (blend) {
                    case setup -> {
                        color.a = setup.a;
                        return;
                    }
                    case first -> color.a += (setup.a - color.a) * alpha;
                }
                return;
            }
            float a = getCurveValue(time);
            if (alpha == 1)
                slot.color.a = a;
            else {
                if (blend == setup) slot.color.a = slot.data.color.a;
                slot.color.a += (a - slot.color.a) * alpha;
            }
        }
    }

    static public class RGBA2Timeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 8;
        static private final int R = 1, G = 2, B = 3, A = 4, R2 = 5, G2 = 6, B2 = 7;
        final int slotIndex;

        public RGBA2Timeline(int frameCount, int bezierCount, int slotIndex) {
            super(frameCount, bezierCount,
                    Property.rgb.ordinal() + "|" + slotIndex,
                    Property.alpha.ordinal() + "|" + slotIndex,
                    Property.rgb2.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setFrame(int frame, float time, float r, float g, float b, float a, float r2, float g2, float b2) {
            frame <<= 3;
            frames[frame] = time;
            frames[frame + R] = r;
            frames[frame + G] = g;
            frames[frame + B] = b;
            frames[frame + A] = a;
            frames[frame + R2] = r2;
            frames[frame + G2] = g2;
            frames[frame + B2] = b2;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                Color light = slot.color, dark = slot.darkColor, setupLight = slot.data.color, setupDark = slot.data.darkColor;
                switch (blend) {
                    case setup -> {
                        light.set(setupLight);
                        dark.r = setupDark.r;
                        dark.g = setupDark.g;
                        dark.b = setupDark.b;
                        return;
                    }
                    case first -> {
                        light.add((setupLight.r - light.r) * alpha, (setupLight.g - light.g) * alpha, (setupLight.b - light.b) * alpha,
                                (setupLight.a - light.a) * alpha);
                        dark.r += (setupDark.r - dark.r) * alpha;
                        dark.g += (setupDark.g - dark.g) * alpha;
                        dark.b += (setupDark.b - dark.b) * alpha;
                    }
                }
                return;
            }
            float r, g, b, a, r2, g2, b2;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i >> 3];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    a = frames[i + A];
                    r2 = frames[i + R2];
                    g2 = frames[i + G2];
                    b2 = frames[i + B2];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    r += (frames[i + ENTRIES + R] - r) * t;
                    g += (frames[i + ENTRIES + G] - g) * t;
                    b += (frames[i + ENTRIES + B] - b) * t;
                    a += (frames[i + ENTRIES + A] - a) * t;
                    r2 += (frames[i + ENTRIES + R2] - r2) * t;
                    g2 += (frames[i + ENTRIES + G2] - g2) * t;
                    b2 += (frames[i + ENTRIES + B2] - b2) * t;
                }
                case STEPPED -> {
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    a = frames[i + A];
                    r2 = frames[i + R2];
                    g2 = frames[i + G2];
                    b2 = frames[i + B2];
                }
                default -> {
                    r = getBezierValue(time, i, R, curveType - BEZIER);
                    g = getBezierValue(time, i, G, curveType + BEZIER_SIZE - BEZIER);
                    b = getBezierValue(time, i, B, curveType + BEZIER_SIZE * 2 - BEZIER);
                    a = getBezierValue(time, i, A, curveType + BEZIER_SIZE * 3 - BEZIER);
                    r2 = getBezierValue(time, i, R2, curveType + BEZIER_SIZE * 4 - BEZIER);
                    g2 = getBezierValue(time, i, G2, curveType + BEZIER_SIZE * 5 - BEZIER);
                    b2 = getBezierValue(time, i, B2, curveType + BEZIER_SIZE * 6 - BEZIER);
                }
            }
            Color light = slot.color, dark = slot.darkColor;
            if (alpha == 1) {
                slot.color.set(r, g, b, a);
                dark.r = r2;
                dark.g = g2;
                dark.b = b2;
            } else {
                if (blend == setup) {
                    light.set(slot.data.color);
                    dark.set(slot.data.darkColor);
                }
                light.add((r - light.r) * alpha, (g - light.g) * alpha, (b - light.b) * alpha, (a - light.a) * alpha);
                dark.r += (r2 - dark.r) * alpha;
                dark.g += (g2 - dark.g) * alpha;
                dark.b += (b2 - dark.b) * alpha;
            }
        }
    }

    static public class RGB2Timeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 7;
        static private final int R = 1, G = 2, B = 3, R2 = 4, G2 = 5, B2 = 6;
        final int slotIndex;

        public RGB2Timeline(int frameCount, int bezierCount, int slotIndex) {
            super(frameCount, bezierCount,
                    Property.rgb.ordinal() + "|" + slotIndex,
                    Property.rgb2.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setFrame(int frame, float time, float r, float g, float b, float r2, float g2, float b2) {
            frame *= ENTRIES;
            frames[frame] = time;
            frames[frame + R] = r;
            frames[frame + G] = g;
            frames[frame + B] = b;
            frames[frame + R2] = r2;
            frames[frame + G2] = g2;
            frames[frame + B2] = b2;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                Color light = slot.color, dark = slot.darkColor, setupLight = slot.data.color, setupDark = slot.data.darkColor;
                switch (blend) {
                    case setup -> {
                        light.r = setupLight.r;
                        light.g = setupLight.g;
                        light.b = setupLight.b;
                        dark.r = setupDark.r;
                        dark.g = setupDark.g;
                        dark.b = setupDark.b;
                        return;
                    }
                    case first -> {
                        light.r += (setupLight.r - light.r) * alpha;
                        light.g += (setupLight.g - light.g) * alpha;
                        light.b += (setupLight.b - light.b) * alpha;
                        dark.r += (setupDark.r - dark.r) * alpha;
                        dark.g += (setupDark.g - dark.g) * alpha;
                        dark.b += (setupDark.b - dark.b) * alpha;
                    }
                }
                return;
            }
            float r, g, b, r2, g2, b2;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    r2 = frames[i + R2];
                    g2 = frames[i + G2];
                    b2 = frames[i + B2];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    r += (frames[i + ENTRIES + R] - r) * t;
                    g += (frames[i + ENTRIES + G] - g) * t;
                    b += (frames[i + ENTRIES + B] - b) * t;
                    r2 += (frames[i + ENTRIES + R2] - r2) * t;
                    g2 += (frames[i + ENTRIES + G2] - g2) * t;
                    b2 += (frames[i + ENTRIES + B2] - b2) * t;
                }
                case STEPPED -> {
                    r = frames[i + R];
                    g = frames[i + G];
                    b = frames[i + B];
                    r2 = frames[i + R2];
                    g2 = frames[i + G2];
                    b2 = frames[i + B2];
                }
                default -> {
                    r = getBezierValue(time, i, R, curveType - BEZIER);
                    g = getBezierValue(time, i, G, curveType + BEZIER_SIZE - BEZIER);
                    b = getBezierValue(time, i, B, curveType + BEZIER_SIZE * 2 - BEZIER);
                    r2 = getBezierValue(time, i, R2, curveType + BEZIER_SIZE * 3 - BEZIER);
                    g2 = getBezierValue(time, i, G2, curveType + BEZIER_SIZE * 4 - BEZIER);
                    b2 = getBezierValue(time, i, B2, curveType + BEZIER_SIZE * 5 - BEZIER);
                }
            }
            Color light = slot.color, dark = slot.darkColor;
            if (alpha == 1) {
                light.r = r;
                light.g = g;
                light.b = b;
                dark.r = r2;
                dark.g = g2;
                dark.b = b2;
            } else {
                if (blend == setup) {
                    Color setupLight = slot.data.color, setupDark = slot.data.darkColor;
                    light.r = setupLight.r;
                    light.g = setupLight.g;
                    light.b = setupLight.b;
                    dark.r = setupDark.r;
                    dark.g = setupDark.g;
                    dark.b = setupDark.b;
                }
                light.r += (r - light.r) * alpha;
                light.g += (g - light.g) * alpha;
                light.b += (b - light.b) * alpha;
                dark.r += (r2 - dark.r) * alpha;
                dark.g += (g2 - dark.g) * alpha;
                dark.b += (b2 - dark.b) * alpha;
            }
        }
    }

    static public class AttachmentTimeline extends Timeline implements SlotTimeline {
        final int slotIndex;
        final String[] attachmentNames;

        public AttachmentTimeline(int frameCount, int slotIndex) {
            super(frameCount, Property.attachment.ordinal() + "|" + slotIndex);
            this.slotIndex = slotIndex;
            attachmentNames = new String[frameCount];
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        // public String[] getAttachmentNames() {
        //     return attachmentNames;
        // }

        public void setFrame(int frame, float time, String attachmentName) {
            frames[frame] = time;
            attachmentNames[frame] = attachmentName;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            if (direction == out) {
                if (blend == setup) setAttachment(skeleton, slot, slot.data.attachmentName);
                return;
            }
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (blend == setup || blend == first) setAttachment(skeleton, slot, slot.data.attachmentName);
                return;
            }
            setAttachment(skeleton, slot, attachmentNames[search(frames, time)]);
        }

        private void setAttachment(Skeleton skeleton, Slot slot, String attachmentName) {
            slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
        }
    }

    static public class DeformTimeline extends CurveTimeline implements SlotTimeline {
        final int slotIndex;
        final VertexAttachment attachment;
        private final float[][] vertices;

        public DeformTimeline(int frameCount, int bezierCount, int slotIndex, VertexAttachment attachment) {
            super(frameCount, bezierCount, Property.deform.ordinal() + "|" + slotIndex + "|" + attachment.getId());
            this.slotIndex = slotIndex;
            this.attachment = attachment;
            vertices = new float[frameCount][];
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        // public VertexAttachment getAttachment() {
        //     return attachment;
        // }

        // public float[][] getVertices() {
        //     return vertices;
        // }

        public void setFrame(int frame, float time, float[] vertices) {
            frames[frame] = time;
            this.vertices[frame] = vertices;
        }

        public void setBezier(int bezier, int frame, int value, float time1, float value1, float cx1, float cy1, float cx2,
                              float cy2, float time2, float value2) {
            float[] curves = this.curves;
            int i = getFrameCount() + bezier * BEZIER_SIZE;
            if (value == 0) curves[frame] = BEZIER + i;
            float tmpx = (time1 - cx1 * 2 + cx2) * 0.03f, tmpy = cy2 * 0.03f - cy1 * 0.06f;
            float dddx = ((cx1 - cx2) * 3 - time1 + time2) * 0.006f, dddy = (cy1 - cy2 + 0.33333333f) * 0.018f;
            float ddx = tmpx * 2 + dddx, ddy = tmpy * 2 + dddy;
            float dx = (cx1 - time1) * 0.3f + tmpx + dddx * 0.16666667f, dy = cy1 * 0.3f + tmpy + dddy * 0.16666667f;
            float x = time1 + dx, y = dy;
            for (int n = i + BEZIER_SIZE; i < n; i += 2) {
                curves[i] = x;
                curves[i + 1] = y;
                dx += ddx;
                dy += ddy;
                ddx += dddx;
                ddy += dddy;
                x += dx;
                y += dy;
            }
        }

        private float getCurvePercent(float time, int frame) {
            float[] curves = this.curves;
            int i = (int) curves[frame];
            switch (i) {
                case LINEAR:
                    float x = frames[frame];
                    return (time - x) / (frames[frame + getFrameEntries()] - x);
                case STEPPED:
                    return 0;
            }
            i -= BEZIER;
            if (curves[i] > time) {
                float x = frames[frame];
                return curves[i + 1] * (time - x) / (curves[i] - x);
            }
            int n = i + BEZIER_SIZE;
            for (i += 2; i < n; i += 2) {
                if (curves[i] >= time) {
                    float x = curves[i - 2], y = curves[i - 1];
                    return y + (time - x) / (curves[i] - x) * (curves[i + 1] - y);
                }
            }
            float x = curves[n - 2], y = curves[n - 1];
            return y + (1 - y) * (time - x) / (frames[frame + getFrameEntries()] - x);
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Slot slot = skeleton.slots.get(slotIndex);
            if (!slot.bone.active) return;
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment)
                    || ((VertexAttachment) slotAttachment).getDeformAttachment() != attachment) return;
            FloatArray deformArray = slot.getDeform();
            if (deformArray.size == 0) blend = setup;
            float[][] vertices = this.vertices;
            int vertexCount = vertices[0].length;
            float[] frames = this.frames;
            if (time < frames[0]) {
                VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                switch (blend) {
                    case setup -> {
                        deformArray.clear();
                        return;
                    }
                    case first -> {
                        if (alpha == 1) {
                            deformArray.clear();
                            return;
                        }
                        float[] deform = deformArray.setSize(vertexCount);
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++)
                                deform[i] += (setupVertices[i] - deform[i]) * alpha;
                        } else {
                            alpha = 1 - alpha;
                            for (int i = 0; i < vertexCount; i++)
                                deform[i] *= alpha;
                        }
                    }
                }
                return;
            }
            float[] deform = deformArray.setSize(vertexCount);
            if (time >= frames[frames.length - 1]) {
                float[] lastVertices = vertices[frames.length - 1];
                if (alpha == 1) {
                    if (blend == add) {
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++)
                                deform[i] += lastVertices[i] - setupVertices[i];
                        } else {
                            for (int i = 0; i < vertexCount; i++)
                                deform[i] += lastVertices[i];
                        }
                    } else {
                        arraycopy(lastVertices, 0, deform, 0, vertexCount);
                    }
                } else {
                    switch (blend) {
                        case setup: {
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {
                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++) {
                                    float setup = setupVertices[i];
                                    deform[i] = setup + (lastVertices[i] - setup) * alpha;
                                }
                            } else {
                                for (int i = 0; i < vertexCount; i++)
                                    deform[i] = lastVertices[i] * alpha;
                            }
                            break;
                        }
                        case first:
                        case replace:
                            for (int i = 0; i < vertexCount; i++)
                                deform[i] += (lastVertices[i] - deform[i]) * alpha;
                            break;
                        case add:
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {
                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++)
                                    deform[i] += (lastVertices[i] - setupVertices[i]) * alpha;
                            } else {
                                for (int i = 0; i < vertexCount; i++)
                                    deform[i] += lastVertices[i] * alpha;
                            }
                    }
                }
                return;
            }
            int frame = search(frames, time);
            float percent = getCurvePercent(time, frame);
            float[] prevVertices = vertices[frame];
            float[] nextVertices = vertices[frame + 1];
            if (alpha == 1) {
                if (blend == add) {
                    VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                    if (vertexAttachment.getBones() == null) {
                        float[] setupVertices = vertexAttachment.getVertices();
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            deform[i] += prev + (nextVertices[i] - prev) * percent - setupVertices[i];
                        }
                    } else {
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            deform[i] += prev + (nextVertices[i] - prev) * percent;
                        }
                    }
                } else {
                    for (int i = 0; i < vertexCount; i++) {
                        float prev = prevVertices[i];
                        deform[i] = prev + (nextVertices[i] - prev) * percent;
                    }
                }
            } else {
                switch (blend) {
                    case setup: {
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i], setup = setupVertices[i];
                                deform[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha;
                            }
                        } else {
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                deform[i] = (prev + (nextVertices[i] - prev) * percent) * alpha;
                            }
                        }
                        break;
                    }
                    case first:
                    case replace:
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            deform[i] += (prev + (nextVertices[i] - prev) * percent - deform[i]) * alpha;
                        }
                        break;
                    case add:
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                deform[i] += (prev + (nextVertices[i] - prev) * percent - setupVertices[i]) * alpha;
                            }
                        } else {
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                deform[i] += (prev + (nextVertices[i] - prev) * percent) * alpha;
                            }
                        }
                }
            }
        }
    }

    static public class EventTimeline extends Timeline {
        static private final String[] propertyIds = {Integer.toString(Property.event.ordinal())};
        private final Event[] events;

        public EventTimeline(int frameCount) {
            super(frameCount, propertyIds);
            events = new Event[frameCount];
        }

        // public Event[] getEvents() {
        //     return events;
        // }

        public void setFrame(int frame, Event event) {
            frames[frame] = event.time;
            events[frame] = event;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> firedEvents, float alpha,
                          MixBlend blend, MixDirection direction) {
            if (firedEvents == null) return;
            float[] frames = this.frames;
            int frameCount = frames.length;
            if (lastTime > time) {
                apply(skeleton, lastTime, Integer.MAX_VALUE, firedEvents, alpha, blend, direction);
                lastTime = -1f;
            } else if (lastTime >= frames[frameCount - 1])
                return;
            if (time < frames[0]) return;
            int i;
            if (lastTime < frames[0])
                i = 0;
            else {
                i = search(frames, lastTime) + 1;
                float frameTime = frames[i];
                while (i > 0) {
                    if (frames[i - 1] != frameTime) break;
                    i--;
                }
            }
            for (; i < frameCount && time >= frames[i]; i++)
                firedEvents.add(events[i]);
        }
    }

    static public class DrawOrderTimeline extends Timeline {
        static private final String[] propertyIds = {Integer.toString(Property.drawOrder.ordinal())};
        private final int[][] drawOrders;

        public DrawOrderTimeline(int frameCount) {
            super(frameCount, propertyIds);
            drawOrders = new int[frameCount][];
        }

        // public int[][] getDrawOrders() {
        //     return drawOrders;
        // }

        public void setFrame(int frame, float time, @Null int[] drawOrder) {
            frames[frame] = time;
            drawOrders[frame] = drawOrder;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Object[] drawOrder = skeleton.drawOrder.items;
            if (direction == out) {
                if (blend == setup) arraycopy(skeleton.slots.items, 0, drawOrder, 0, skeleton.slots.size);
                return;
            }
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (blend == setup || blend == first)
                    arraycopy(skeleton.slots.items, 0, drawOrder, 0, skeleton.slots.size);
                return;
            }
            int[] drawOrderToSetupIndex = drawOrders[search(frames, time)];
            if (drawOrderToSetupIndex == null)
                arraycopy(skeleton.slots.items, 0, drawOrder, 0, skeleton.slots.size);
            else {
                Object[] slots = skeleton.slots.items;
                for (int i = 0, n = drawOrderToSetupIndex.length; i < n; i++)
                    drawOrder[i] = slots[drawOrderToSetupIndex[i]];
            }
        }
    }

    static public class IkConstraintTimeline extends CurveTimeline {
        static public final int ENTRIES = 6;
        static private final int MIX = 1, SOFTNESS = 2, BEND_DIRECTION = 3, COMPRESS = 4, STRETCH = 5;
        final int ikConstraintIndex;

        public IkConstraintTimeline(int frameCount, int bezierCount, int ikConstraintIndex) {
            super(frameCount, bezierCount, Property.ikConstraint.ordinal() + "|" + ikConstraintIndex);
            this.ikConstraintIndex = ikConstraintIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        // public int getIkConstraintIndex() {
        //     return ikConstraintIndex;
        // }

        public void setFrame(int frame, float time, float mix, float softness, int bendDirection, boolean compress,
                             boolean stretch) {
            frame *= ENTRIES;
            frames[frame] = time;
            frames[frame + MIX] = mix;
            frames[frame + SOFTNESS] = softness;
            frames[frame + BEND_DIRECTION] = bendDirection;
            frames[frame + COMPRESS] = compress ? 1 : 0;
            frames[frame + STRETCH] = stretch ? 1 : 0;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            if (!constraint.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.mix = constraint.data.mix;
                        constraint.softness = constraint.data.softness;
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                        return;
                    }
                    case first -> {
                        constraint.mix += (constraint.data.mix - constraint.mix) * alpha;
                        constraint.softness += (constraint.data.softness - constraint.softness) * alpha;
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                    }
                }
                return;
            }
            float mix, softness;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    mix = frames[i + MIX];
                    softness = frames[i + SOFTNESS];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    mix += (frames[i + ENTRIES + MIX] - mix) * t;
                    softness += (frames[i + ENTRIES + SOFTNESS] - softness) * t;
                }
                case STEPPED -> {
                    mix = frames[i + MIX];
                    softness = frames[i + SOFTNESS];
                }
                default -> {
                    mix = getBezierValue(time, i, MIX, curveType - BEZIER);
                    softness = getBezierValue(time, i, SOFTNESS, curveType + BEZIER_SIZE - BEZIER);
                }
            }
            if (blend == setup) {
                constraint.mix = constraint.data.mix + (mix - constraint.data.mix) * alpha;
                constraint.softness = constraint.data.softness + (softness - constraint.data.softness) * alpha;
                if (direction == out) {
                    constraint.bendDirection = constraint.data.bendDirection;
                    constraint.compress = constraint.data.compress;
                    constraint.stretch = constraint.data.stretch;
                } else {
                    constraint.bendDirection = (int) frames[i + BEND_DIRECTION];
                    constraint.compress = frames[i + COMPRESS] != 0;
                    constraint.stretch = frames[i + STRETCH] != 0;
                }
            } else {
                constraint.mix += (mix - constraint.mix) * alpha;
                constraint.softness += (softness - constraint.softness) * alpha;
                if (direction == in) {
                    constraint.bendDirection = (int) frames[i + BEND_DIRECTION];
                    constraint.compress = frames[i + COMPRESS] != 0;
                    constraint.stretch = frames[i + STRETCH] != 0;
                }
            }
        }
    }

    static public class TransformConstraintTimeline extends CurveTimeline {
        static public final int ENTRIES = 7;
        static private final int ROTATE = 1, X = 2, Y = 3, SCALEX = 4, SCALEY = 5, SHEARY = 6;
        final int transformConstraintIndex;

        public TransformConstraintTimeline(int frameCount, int bezierCount, int transformConstraintIndex) {
            super(frameCount, bezierCount, Property.transformConstraint.ordinal() + "|" + transformConstraintIndex);
            this.transformConstraintIndex = transformConstraintIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        // public int getTransformConstraintIndex() {
        //     return transformConstraintIndex;
        // }

        public void setFrame(int frame, float time, float mixRotate, float mixX, float mixY, float mixScaleX, float mixScaleY,
                             float mixShearY) {
            frame *= ENTRIES;
            frames[frame] = time;
            frames[frame + ROTATE] = mixRotate;
            frames[frame + X] = mixX;
            frames[frame + Y] = mixY;
            frames[frame + SCALEX] = mixScaleX;
            frames[frame + SCALEY] = mixScaleY;
            frames[frame + SHEARY] = mixShearY;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);
            if (!constraint.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                TransformConstraintData data = constraint.data;
                switch (blend) {
                    case setup -> {
                        constraint.mixRotate = data.mixRotate;
                        constraint.mixX = data.mixX;
                        constraint.mixY = data.mixY;
                        constraint.mixScaleX = data.mixScaleX;
                        constraint.mixScaleY = data.mixScaleY;
                        constraint.mixShearY = data.mixShearY;
                        return;
                    }
                    case first -> {
                        constraint.mixRotate += (data.mixRotate - constraint.mixRotate) * alpha;
                        constraint.mixX += (data.mixX - constraint.mixX) * alpha;
                        constraint.mixY += (data.mixY - constraint.mixY) * alpha;
                        constraint.mixScaleX += (data.mixScaleX - constraint.mixScaleX) * alpha;
                        constraint.mixScaleY += (data.mixScaleY - constraint.mixScaleY) * alpha;
                        constraint.mixShearY += (data.mixShearY - constraint.mixShearY) * alpha;
                    }
                }
                return;
            }
            float rotate, x, y, scaleX, scaleY, shearY;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i / ENTRIES];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    rotate = frames[i + ROTATE];
                    x = frames[i + X];
                    y = frames[i + Y];
                    scaleX = frames[i + SCALEX];
                    scaleY = frames[i + SCALEY];
                    shearY = frames[i + SHEARY];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    rotate += (frames[i + ENTRIES + ROTATE] - rotate) * t;
                    x += (frames[i + ENTRIES + X] - x) * t;
                    y += (frames[i + ENTRIES + Y] - y) * t;
                    scaleX += (frames[i + ENTRIES + SCALEX] - scaleX) * t;
                    scaleY += (frames[i + ENTRIES + SCALEY] - scaleY) * t;
                    shearY += (frames[i + ENTRIES + SHEARY] - shearY) * t;
                }
                case STEPPED -> {
                    rotate = frames[i + ROTATE];
                    x = frames[i + X];
                    y = frames[i + Y];
                    scaleX = frames[i + SCALEX];
                    scaleY = frames[i + SCALEY];
                    shearY = frames[i + SHEARY];
                }
                default -> {
                    rotate = getBezierValue(time, i, ROTATE, curveType - BEZIER);
                    x = getBezierValue(time, i, X, curveType + BEZIER_SIZE - BEZIER);
                    y = getBezierValue(time, i, Y, curveType + BEZIER_SIZE * 2 - BEZIER);
                    scaleX = getBezierValue(time, i, SCALEX, curveType + BEZIER_SIZE * 3 - BEZIER);
                    scaleY = getBezierValue(time, i, SCALEY, curveType + BEZIER_SIZE * 4 - BEZIER);
                    shearY = getBezierValue(time, i, SHEARY, curveType + BEZIER_SIZE * 5 - BEZIER);
                }
            }
            if (blend == setup) {
                TransformConstraintData data = constraint.data;
                constraint.mixRotate = data.mixRotate + (rotate - data.mixRotate) * alpha;
                constraint.mixX = data.mixX + (x - data.mixX) * alpha;
                constraint.mixY = data.mixY + (y - data.mixY) * alpha;
                constraint.mixScaleX = data.mixScaleX + (scaleX - data.mixScaleX) * alpha;
                constraint.mixScaleY = data.mixScaleY + (scaleY - data.mixScaleY) * alpha;
                constraint.mixShearY = data.mixShearY + (shearY - data.mixShearY) * alpha;
            } else {
                constraint.mixRotate += (rotate - constraint.mixRotate) * alpha;
                constraint.mixX += (x - constraint.mixX) * alpha;
                constraint.mixY += (y - constraint.mixY) * alpha;
                constraint.mixScaleX += (scaleX - constraint.mixScaleX) * alpha;
                constraint.mixScaleY += (scaleY - constraint.mixScaleY) * alpha;
                constraint.mixShearY += (shearY - constraint.mixShearY) * alpha;
            }
        }
    }

    static public class PathConstraintPositionTimeline extends CurveTimeline1 {
        final int pathConstraintIndex;

        public PathConstraintPositionTimeline(int frameCount, int bezierCount, int pathConstraintIndex) {
            super(frameCount, bezierCount, Property.pathConstraintPosition.ordinal() + "|" + pathConstraintIndex);
            this.pathConstraintIndex = pathConstraintIndex;
        }

        // public int getPathConstraintIndex() {
        //     return pathConstraintIndex;
        // }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            if (!constraint.active) return;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.position = constraint.data.position;
                        return;
                    }
                    case first -> constraint.position += (constraint.data.position - constraint.position) * alpha;
                }
                return;
            }
            float position = getCurveValue(time);
            if (blend == setup)
                constraint.position = constraint.data.position + (position - constraint.data.position) * alpha;
            else
                constraint.position += (position - constraint.position) * alpha;
        }
    }

    static public class PathConstraintSpacingTimeline extends CurveTimeline1 {
        final int pathConstraintIndex;

        public PathConstraintSpacingTimeline(int frameCount, int bezierCount, int pathConstraintIndex) {
            super(frameCount, bezierCount, Property.pathConstraintSpacing.ordinal() + "|" + pathConstraintIndex);
            this.pathConstraintIndex = pathConstraintIndex;
        }

        // public int getPathConstraintIndex() {
        //     return pathConstraintIndex;
        // }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            if (!constraint.active) return;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.spacing = constraint.data.spacing;
                        return;
                    }
                    case first -> constraint.spacing += (constraint.data.spacing - constraint.spacing) * alpha;
                }
                return;
            }
            float spacing = getCurveValue(time);
            if (blend == setup)
                constraint.spacing = constraint.data.spacing + (spacing - constraint.data.spacing) * alpha;
            else
                constraint.spacing += (spacing - constraint.spacing) * alpha;
        }
    }

    static public class PathConstraintMixTimeline extends CurveTimeline {
        static public final int ENTRIES = 4;
        static private final int ROTATE = 1, X = 2, Y = 3;
        final int pathConstraintIndex;

        public PathConstraintMixTimeline(int frameCount, int bezierCount, int pathConstraintIndex) {
            super(frameCount, bezierCount, Property.pathConstraintMix.ordinal() + "|" + pathConstraintIndex);
            this.pathConstraintIndex = pathConstraintIndex;
        }

        public int getFrameEntries() {
            return ENTRIES;
        }

        // public int getPathConstraintIndex() {
        //     return pathConstraintIndex;
        // }

        public void setFrame(int frame, float time, float mixRotate, float mixX, float mixY) {
            frame <<= 2;
            frames[frame] = time;
            frames[frame + ROTATE] = mixRotate;
            frames[frame + X] = mixX;
            frames[frame + Y] = mixY;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            if (!constraint.active) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.mixRotate = constraint.data.mixRotate;
                        constraint.mixX = constraint.data.mixX;
                        constraint.mixY = constraint.data.mixY;
                        return;
                    }
                    case first -> {
                        constraint.mixRotate += (constraint.data.mixRotate - constraint.mixRotate) * alpha;
                        constraint.mixX += (constraint.data.mixX - constraint.mixX) * alpha;
                        constraint.mixY += (constraint.data.mixY - constraint.mixY) * alpha;
                    }
                }
                return;
            }
            float rotate, x, y;
            int i = search(frames, time, ENTRIES), curveType = (int) curves[i >> 2];
            switch (curveType) {
                case LINEAR -> {
                    float before = frames[i];
                    rotate = frames[i + ROTATE];
                    x = frames[i + X];
                    y = frames[i + Y];
                    float t = (time - before) / (frames[i + ENTRIES] - before);
                    rotate += (frames[i + ENTRIES + ROTATE] - rotate) * t;
                    x += (frames[i + ENTRIES + X] - x) * t;
                    y += (frames[i + ENTRIES + Y] - y) * t;
                }
                case STEPPED -> {
                    rotate = frames[i + ROTATE];
                    x = frames[i + X];
                    y = frames[i + Y];
                }
                default -> {
                    rotate = getBezierValue(time, i, ROTATE, curveType - BEZIER);
                    x = getBezierValue(time, i, X, curveType + BEZIER_SIZE - BEZIER);
                    y = getBezierValue(time, i, Y, curveType + BEZIER_SIZE * 2 - BEZIER);
                }
            }
            if (blend == setup) {
                PathConstraintData data = constraint.data;
                constraint.mixRotate = data.mixRotate + (rotate - data.mixRotate) * alpha;
                constraint.mixX = data.mixX + (x - data.mixX) * alpha;
                constraint.mixY = data.mixY + (y - data.mixY) * alpha;
            } else {
                constraint.mixRotate += (rotate - constraint.mixRotate) * alpha;
                constraint.mixX += (x - constraint.mixX) * alpha;
                constraint.mixY += (y - constraint.mixY) * alpha;
            }
        }
    }
}
