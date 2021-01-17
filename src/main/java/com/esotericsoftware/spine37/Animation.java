package com.esotericsoftware.spine37;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine37.attachments.Attachment;
import com.esotericsoftware.spine37.attachments.VertexAttachment;

import static com.esotericsoftware.spine37.Animation.MixBlend.*;
import static com.esotericsoftware.spine37.Animation.MixDirection.in;
import static com.esotericsoftware.spine37.Animation.MixDirection.out;


public class Animation {
    final String name;
    final Array<Timeline> timelines;
    float duration;

    public Animation(String name, Array<Timeline> timelines, float duration) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        if (timelines == null) throw new IllegalArgumentException("timelines cannot be null.");
        this.name = name;
        this.timelines = timelines;
        this.duration = duration;
    }
    
    static int binarySearch(float[] values, float target, int step) {
        int low = 0;
        int high = values.length / step - 2;
        if (high == 0) return step;
        int current = high >>> 1;
        while (true) {
            if (values[(current + 1) * step] <= target)
                low = current + 1;
            else
                high = current;
            if (low == high) return (low + 1) * step;
            current = (low + high) >>> 1;
        }
    }
    
    static int binarySearch(float[] values, float target) {
        int low = 0;
        int high = values.length - 2;
        if (high == 0) return 1;
        int current = high >>> 1;
        while (true) {
            if (values[current + 1] <= target)
                low = current + 1;
            else
                high = current;
            if (low == high) return low + 1;
            current = (low + high) >>> 1;
        }
    }

    static int linearSearch(float[] values, float target, int step) {
        for (int i = 0, last = values.length - step; i <= last; i += step)
            if (values[i] > target) return i;
        return -1;
    }

    public Array<Timeline> getTimelines() {
        return timelines;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }
    
    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha,
                      MixBlend blend, MixDirection direction) {
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }

        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, alpha, blend, direction);
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

    private enum TimelineType {
        rotate, translate, scale, shear,
        attachment, color, deform,
        event, drawOrder,
        ikConstraint, transformConstraint,
        pathConstraintPosition, pathConstraintSpacing, pathConstraintMix,
        twoColor
    }

    
    public interface Timeline {
        
        void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                   MixDirection direction);

        
        int getPropertyId();
    }

    
    public interface BoneTimeline extends Timeline {
        
        int getBoneIndex();

        void setBoneIndex(int index);
    }

    
    public interface SlotTimeline extends Timeline {
        
        int getSlotIndex();

        void setSlotIndex(int index);
    }

    
    abstract static public class CurveTimeline implements Timeline {
        static public final float LINEAR = 0, STEPPED = 1, BEZIER = 2;
        static private final int BEZIER_SIZE = 10 * 2 - 1;

        private final float[] curves;

        public CurveTimeline(int frameCount) {
            if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be > 0: " + frameCount);
            curves = new float[(frameCount - 1) * BEZIER_SIZE];
        }

        
        public int getFrameCount() {
            return curves.length / BEZIER_SIZE + 1;
        }

        
        public void setLinear(int frameIndex) {
            curves[frameIndex * BEZIER_SIZE] = LINEAR;
        }

        
        public void setStepped(int frameIndex) {
            curves[frameIndex * BEZIER_SIZE] = STEPPED;
        }

        
        public float getCurveType(int frameIndex) {
            int index = frameIndex * BEZIER_SIZE;
            if (index == curves.length) return LINEAR;
            float type = curves[index];
            if (type == LINEAR) return LINEAR;
            if (type == STEPPED) return STEPPED;
            return BEZIER;
        }

        
        public void setCurve(int frameIndex, float cx1, float cy1, float cx2, float cy2) {
            float tmpx = (-cx1 * 2 + cx2) * 0.03f, tmpy = (-cy1 * 2 + cy2) * 0.03f;
            float dddfx = ((cx1 - cx2) * 3 + 1) * 0.006f, dddfy = ((cy1 - cy2) * 3 + 1) * 0.006f;
            float ddfx = tmpx * 2 + dddfx, ddfy = tmpy * 2 + dddfy;
            float dfx = cx1 * 0.3f + tmpx + dddfx * 0.16666667f, dfy = cy1 * 0.3f + tmpy + dddfy * 0.16666667f;

            int i = frameIndex * BEZIER_SIZE;
            float[] curves = this.curves;
            curves[i++] = BEZIER;

            float x = dfx, y = dfy;
            for (int n = i + BEZIER_SIZE - 1; i < n; i += 2) {
                curves[i] = x;
                curves[i + 1] = y;
                dfx += ddfx;
                dfy += ddfy;
                ddfx += dddfx;
                ddfy += dddfy;
                x += dfx;
                y += dfy;
            }
        }

        
        public float getCurvePercent(int frameIndex, float percent) {
            percent = MathUtils.clamp(percent, 0, 1);
            float[] curves = this.curves;
            int i = frameIndex * BEZIER_SIZE;
            float type = curves[i];
            if (type == LINEAR) return percent;
            if (type == STEPPED) return 0;
            i++;
            float x = 0;
            for (int start = i, n = i + BEZIER_SIZE - 1; i < n; i += 2) {
                x = curves[i];
                if (x >= percent) {
                    if (i == start) return curves[i + 1] * percent / x;
                    float prevX = curves[i - 2], prevY = curves[i - 1];
                    return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX);
                }
            }
            float y = curves[i - 1];
            return y + (1 - y) * (percent - x) / (1 - x);
        }
    }

    
    static public class RotateTimeline extends CurveTimeline implements BoneTimeline {
        static public final int ENTRIES = 2;
        static final int PREV_TIME = -2, PREV_ROTATION = -1;
        static final int ROTATION = 1;
        final float[] frames;
        int boneIndex;

        public RotateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount << 1];
        }

        public int getPropertyId() {
            return (TimelineType.rotate.ordinal() << 24) + boneIndex;
        }

        
        public int getBoneIndex() {
            return boneIndex;
        }

        public void setBoneIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.boneIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float degrees) {
            frameIndex <<= 1;
            frames[frameIndex] = time;
            frames[frameIndex + ROTATION] = degrees;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        bone.rotation = bone.data.rotation;
                        return;
                    }
                    case first -> {
                        float r = bone.data.rotation - bone.rotation;
                        bone.rotation += (r - (16384 - (int) (16384.499999999996 - r / 360)) * 360) * alpha;
                    }
                }
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                float r = frames[frames.length + PREV_ROTATION];
                switch (blend) {
                    case setup:
                        bone.rotation = bone.data.rotation + r * alpha;
                        break;
                    case first:
                    case replace:
                        r += bone.data.rotation - bone.rotation;
                        r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;

                    case add:
                        bone.rotation += r * alpha;
                }
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float prevRotation = frames[frame + PREV_ROTATION];
            float frameTime = frames[frame];
            float percent = getCurvePercent((frame >> 1) - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            float r = frames[frame + ROTATION] - prevRotation;
            r = prevRotation + (r - (16384 - (int) (16384.499999999996 - r / 360)) * 360) * percent;
            switch (blend) {
                case setup:
                    bone.rotation = bone.data.rotation + (r - (16384 - (int) (16384.499999999996 - r / 360)) * 360) * alpha;
                    break;
                case first:
                case replace:
                    r += bone.data.rotation - bone.rotation;

                case add:
                    bone.rotation += (r - (16384 - (int) (16384.499999999996 - r / 360)) * 360) * alpha;
            }
        }
    }

    
    static public class TranslateTimeline extends CurveTimeline implements BoneTimeline {
        static public final int ENTRIES = 3;
        static final int PREV_TIME = -3, PREV_X = -2, PREV_Y = -1;
        static final int X = 1, Y = 2;
        final float[] frames;
        int boneIndex;

        public TranslateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.translate.ordinal() << 24) + boneIndex;
        }

        
        public int getBoneIndex() {
            return boneIndex;
        }

        public void setBoneIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.boneIndex = index;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float x, float y) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + X] = x;
            frames[frameIndex + Y] = y;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Bone bone = skeleton.bones.get(boneIndex);
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
            if (time >= frames[frames.length - ENTRIES]) {
                x = frames[frames.length + PREV_X];
                y = frames[frames.length + PREV_Y];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                x = frames[frame + PREV_X];
                y = frames[frame + PREV_Y];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                x += (frames[frame + X] - x) * percent;
                y += (frames[frame + Y] - y) * percent;
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

    
    static public class ScaleTimeline extends TranslateTimeline {
        public ScaleTimeline(int frameCount) {
            super(frameCount);
        }

        public int getPropertyId() {
            return (TimelineType.scale.ordinal() << 24) + boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Bone bone = skeleton.bones.get(boneIndex);
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
            if (time >= frames[frames.length - ENTRIES]) {
                x = frames[frames.length + PREV_X] * bone.data.scaleX;
                y = frames[frames.length + PREV_Y] * bone.data.scaleY;
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                x = frames[frame + PREV_X];
                y = frames[frame + PREV_Y];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                x = (x + (frames[frame + X] - x) * percent) * bone.data.scaleX;
                y = (y + (frames[frame + Y] - y) * percent) * bone.data.scaleY;
            }
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

    
    static public class ShearTimeline extends TranslateTimeline {
        public ShearTimeline(int frameCount) {
            super(frameCount);
        }

        public int getPropertyId() {
            return (TimelineType.shear.ordinal() << 24) + boneIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Bone bone = skeleton.bones.get(boneIndex);
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
            if (time >= frames[frames.length - ENTRIES]) {
                x = frames[frames.length + PREV_X];
                y = frames[frames.length + PREV_Y];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                x = frames[frame + PREV_X];
                y = frames[frame + PREV_Y];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                x = x + (frames[frame + X] - x) * percent;
                y = y + (frames[frame + Y] - y) * percent;
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

    
    static public class ColorTimeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 5;
        static private final int PREV_TIME = -5, PREV_R = -4, PREV_G = -3, PREV_B = -2, PREV_A = -1;
        static private final int R = 1, G = 2, B = 3, A = 4;
        private final float[] frames;
        int slotIndex;

        public ColorTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.color.ordinal() << 24) + slotIndex;
        }

        
        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.slotIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float r, float g, float b, float a) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + R] = r;
            frames[frameIndex + G] = g;
            frames[frameIndex + B] = b;
            frames[frameIndex + A] = a;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Slot slot = skeleton.slots.get(slotIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        slot.color.set(slot.data.color);
                        return;
                    }
                    case first -> {
                        Color color = slot.color, setup = slot.data.color;
                        color.add((setup.r - color.r) * alpha, (setup.g - color.g) * alpha, (setup.b - color.b) * alpha,
                                (setup.a - color.a) * alpha);
                    }
                }
                return;
            }

            float r, g, b, a;
            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                r = frames[i + PREV_R];
                g = frames[i + PREV_G];
                b = frames[i + PREV_B];
                a = frames[i + PREV_A];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                r = frames[frame + PREV_R];
                g = frames[frame + PREV_G];
                b = frames[frame + PREV_B];
                a = frames[frame + PREV_A];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                r += (frames[frame + R] - r) * percent;
                g += (frames[frame + G] - g) * percent;
                b += (frames[frame + B] - b) * percent;
                a += (frames[frame + A] - a) * percent;
            }
            if (alpha == 1)
                slot.color.set(r, g, b, a);
            else {
                Color color = slot.color;
                if (blend == setup) color.set(slot.data.color);
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            }
        }
    }

    
    static public class TwoColorTimeline extends CurveTimeline implements SlotTimeline {
        static public final int ENTRIES = 8;
        static private final int PREV_TIME = -8, PREV_R = -7, PREV_G = -6, PREV_B = -5, PREV_A = -4;
        static private final int PREV_R2 = -3, PREV_G2 = -2, PREV_B2 = -1;
        static private final int R = 1, G = 2, B = 3, A = 4, R2 = 5, G2 = 6, B2 = 7;
        private final float[] frames;
        int slotIndex;

        public TwoColorTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.twoColor.ordinal() << 24) + slotIndex;
        }

        
        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.slotIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float r, float g, float b, float a, float r2, float g2, float b2) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + R] = r;
            frames[frameIndex + G] = g;
            frames[frameIndex + B] = b;
            frames[frameIndex + A] = a;
            frames[frameIndex + R2] = r2;
            frames[frameIndex + G2] = g2;
            frames[frameIndex + B2] = b2;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Slot slot = skeleton.slots.get(slotIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        slot.color.set(slot.data.color);
                        slot.darkColor.set(slot.data.darkColor);
                        return;
                    }
                    case first -> {
                        Color light = slot.color, dark = slot.darkColor, setupLight = slot.data.color, setupDark = slot.data.darkColor;
                        light.add((setupLight.r - light.r) * alpha, (setupLight.g - light.g) * alpha, (setupLight.b - light.b) * alpha,
                                (setupLight.a - light.a) * alpha);
                        dark.add((setupDark.r - dark.r) * alpha, (setupDark.g - dark.g) * alpha, (setupDark.b - dark.b) * alpha, 0);
                    }
                }
                return;
            }

            float r, g, b, a, r2, g2, b2;
            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                r = frames[i + PREV_R];
                g = frames[i + PREV_G];
                b = frames[i + PREV_B];
                a = frames[i + PREV_A];
                r2 = frames[i + PREV_R2];
                g2 = frames[i + PREV_G2];
                b2 = frames[i + PREV_B2];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                r = frames[frame + PREV_R];
                g = frames[frame + PREV_G];
                b = frames[frame + PREV_B];
                a = frames[frame + PREV_A];
                r2 = frames[frame + PREV_R2];
                g2 = frames[frame + PREV_G2];
                b2 = frames[frame + PREV_B2];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                r += (frames[frame + R] - r) * percent;
                g += (frames[frame + G] - g) * percent;
                b += (frames[frame + B] - b) * percent;
                a += (frames[frame + A] - a) * percent;
                r2 += (frames[frame + R2] - r2) * percent;
                g2 += (frames[frame + G2] - g2) * percent;
                b2 += (frames[frame + B2] - b2) * percent;
            }
            if (alpha == 1) {
                slot.color.set(r, g, b, a);
                slot.darkColor.set(r2, g2, b2, 1);
            } else {
                Color light = slot.color, dark = slot.darkColor;
                if (blend == setup) {
                    light.set(slot.data.color);
                    dark.set(slot.data.darkColor);
                }
                light.add((r - light.r) * alpha, (g - light.g) * alpha, (b - light.b) * alpha, (a - light.a) * alpha);
                dark.add((r2 - dark.r) * alpha, (g2 - dark.g) * alpha, (b2 - dark.b) * alpha, 0);
            }
        }
    }

    
    static public class AttachmentTimeline implements SlotTimeline {
        final float[] frames;
        final String[] attachmentNames;
        int slotIndex;

        public AttachmentTimeline(int frameCount) {
            frames = new float[frameCount];
            attachmentNames = new String[frameCount];
        }

        public int getPropertyId() {
            return (TimelineType.attachment.ordinal() << 24) + slotIndex;
        }

        
        public int getFrameCount() {
            return frames.length;
        }

        
        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.slotIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public String[] getAttachmentNames() {
            return attachmentNames;
        }

        
        public void setFrame(int frameIndex, float time, String attachmentName) {
            frames[frameIndex] = time;
            attachmentNames[frameIndex] = attachmentName;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Slot slot = skeleton.slots.get(slotIndex);
            if (direction == out && blend == setup) {
                String attachmentName = slot.data.attachmentName;
                slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (blend == setup || blend == first) {
                    String attachmentName = slot.data.attachmentName;
                    slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                }
                return;
            }

            int frameIndex;
            if (time >= frames[frames.length - 1])
                frameIndex = frames.length - 1;
            else
                frameIndex = binarySearch(frames, time) - 1;

            String attachmentName = attachmentNames[frameIndex];
            slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
        }
    }

    
    static public class DeformTimeline extends CurveTimeline implements SlotTimeline {
        private final float[] frames;
        private final float[][] frameVertices;
        int slotIndex;
        VertexAttachment attachment;

        public DeformTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount];
            frameVertices = new float[frameCount][];
        }

        public int getPropertyId() {
            return (TimelineType.deform.ordinal() << 27) + attachment.getId() + slotIndex;
        }

        
        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.slotIndex = index;
        }

        
        public VertexAttachment getAttachment() {
            return attachment;
        }

        public void setAttachment(VertexAttachment attachment) {
            this.attachment = attachment;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public float[][] getVertices() {
            return frameVertices;
        }

        
        public void setFrame(int frameIndex, float time, float[] vertices) {
            frames[frameIndex] = time;
            frameVertices[frameIndex] = vertices;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Slot slot = skeleton.slots.get(slotIndex);
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment) slotAttachment).applyDeform(attachment))
                return;

            FloatArray verticesArray = slot.getAttachmentVertices();
            if (verticesArray.size == 0) blend = setup;

            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;

            float[] frames = this.frames;
            if (time < frames[0]) {
                VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                switch (blend) {
                    case setup -> {
                        verticesArray.clear();
                        return;
                    }
                    case first -> {
                        if (alpha == 1) {
                            verticesArray.clear();
                            return;
                        }
                        float[] vertices = verticesArray.setSize(vertexCount);
                        if (vertexAttachment.getBones() == null) {

                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++)
                                vertices[i] += (setupVertices[i] - vertices[i]) * alpha;
                        } else {

                            alpha = 1 - alpha;
                            for (int i = 0; i < vertexCount; i++)
                                vertices[i] *= alpha;
                        }
                    }
                }
                return;
            }

            float[] vertices = verticesArray.setSize(vertexCount);

            if (time >= frames[frames.length - 1]) {
                float[] lastVertices = frameVertices[frames.length - 1];
                if (alpha == 1) {
                    if (blend == add) {
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {

                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++)
                                vertices[i] += lastVertices[i] - setupVertices[i];
                        } else {

                            for (int i = 0; i < vertexCount; i++)
                                vertices[i] += lastVertices[i];
                        }
                    } else {

                        System.arraycopy(lastVertices, 0, vertices, 0, vertexCount);
                    }
                } else {
                    switch (blend) {
                        case setup: {
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {

                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++) {
                                    float setup = setupVertices[i];
                                    vertices[i] = setup + (lastVertices[i] - setup) * alpha;
                                }
                            } else {

                                for (int i = 0; i < vertexCount; i++)
                                    vertices[i] = lastVertices[i] * alpha;
                            }
                            break;
                        }
                        case first:
                        case replace:

                            for (int i = 0; i < vertexCount; i++)
                                vertices[i] += (lastVertices[i] - vertices[i]) * alpha;
                            break;
                        case add:
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {

                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++)
                                    vertices[i] += (lastVertices[i] - setupVertices[i]) * alpha;
                            } else {

                                for (int i = 0; i < vertexCount; i++)
                                    vertices[i] += lastVertices[i] * alpha;
                            }
                    }
                }
                return;
            }


            int frame = binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime));

            if (alpha == 1) {
                if (blend == add) {
                    VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                    if (vertexAttachment.getBones() == null) {

                        float[] setupVertices = vertexAttachment.getVertices();
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            vertices[i] += prev + (nextVertices[i] - prev) * percent - setupVertices[i];
                        }
                    } else {

                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            vertices[i] += prev + (nextVertices[i] - prev) * percent;
                        }
                    }
                } else {

                    for (int i = 0; i < vertexCount; i++) {
                        float prev = prevVertices[i];
                        vertices[i] = prev + (nextVertices[i] - prev) * percent;
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
                                vertices[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha;
                            }
                        } else {

                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                vertices[i] = (prev + (nextVertices[i] - prev) * percent) * alpha;
                            }
                        }
                        break;
                    }
                    case first:
                    case replace:

                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            vertices[i] += (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                        }
                        break;
                    case add:
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {

                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                vertices[i] += (prev + (nextVertices[i] - prev) * percent - setupVertices[i]) * alpha;
                            }
                        } else {

                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                vertices[i] += (prev + (nextVertices[i] - prev) * percent) * alpha;
                            }
                        }
                }
            }
        }
    }

    
    static public class EventTimeline implements Timeline {
        private final float[] frames;
        private final Event[] events;

        public EventTimeline(int frameCount) {
            frames = new float[frameCount];
            events = new Event[frameCount];
        }

        public int getPropertyId() {
            return TimelineType.event.ordinal() << 24;
        }

        
        public int getFrameCount() {
            return frames.length;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public Event[] getEvents() {
            return events;
        }

        
        public void setFrame(int frameIndex, Event event) {
            frames[frameIndex] = event.time;
            events[frameIndex] = event;
        }

        
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha, MixBlend blend,
                          MixDirection direction) {

            if (firedEvents == null) return;
            float[] frames = this.frames;
            int frameCount = frames.length;

            if (lastTime > time) {
                apply(skeleton, lastTime, Integer.MAX_VALUE, firedEvents, alpha, blend, direction);
                lastTime = -1f;
            } else if (lastTime >= frames[frameCount - 1])
                return;
            if (time < frames[0]) return;

            int frame;
            if (lastTime < frames[0])
                frame = 0;
            else {
                frame = binarySearch(frames, lastTime);
                float frameTime = frames[frame];
                while (frame > 0) {
                    if (frames[frame - 1] != frameTime) break;
                    frame--;
                }
            }
            for (; frame < frameCount && time >= frames[frame]; frame++)
                firedEvents.add(events[frame]);
        }
    }

    
    static public class DrawOrderTimeline implements Timeline {
        private final float[] frames;
        private final int[][] drawOrders;

        public DrawOrderTimeline(int frameCount) {
            frames = new float[frameCount];
            drawOrders = new int[frameCount][];
        }

        public int getPropertyId() {
            return TimelineType.drawOrder.ordinal() << 24;
        }

        
        public int getFrameCount() {
            return frames.length;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public int[][] getDrawOrders() {
            return drawOrders;
        }

        
        public void setFrame(int frameIndex, float time, int[] drawOrder) {
            frames[frameIndex] = time;
            drawOrders[frameIndex] = drawOrder;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
            if (direction == out && blend == setup) {
                System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (blend == setup || blend == first) System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                return;
            }

            int frame;
            if (time >= frames[frames.length - 1])
                frame = frames.length - 1;
            else
                frame = binarySearch(frames, time) - 1;

            int[] drawOrderToSetupIndex = drawOrders[frame];
            if (drawOrderToSetupIndex == null)
                System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
            else {
                for (int i = 0, n = drawOrderToSetupIndex.length; i < n; i++)
                    drawOrder.set(i, slots.get(drawOrderToSetupIndex[i]));
            }
        }
    }

    
    static public class IkConstraintTimeline extends CurveTimeline {
        static public final int ENTRIES = 5;
        static private final int PREV_TIME = -5, PREV_MIX = -4, PREV_BEND_DIRECTION = -3, PREV_COMPRESS = -2, PREV_STRETCH = -1;
        static private final int MIX = 1, BEND_DIRECTION = 2, COMPRESS = 3, STRETCH = 4;
        private final float[] frames;
        int ikConstraintIndex;

        public IkConstraintTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.ikConstraint.ordinal() << 24) + ikConstraintIndex;
        }

        
        public int getIkConstraintIndex() {
            return ikConstraintIndex;
        }

        public void setIkConstraintIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.ikConstraintIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }
        
        public void setFrame(int frameIndex, float time, float mix, int bendDirection, boolean compress, boolean stretch) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + MIX] = mix;
            frames[frameIndex + BEND_DIRECTION] = bendDirection;
            frames[frameIndex + COMPRESS] = compress ? 1 : 0;
            frames[frameIndex + STRETCH] = stretch ? 1 : 0;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.mix = constraint.data.mix;
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                        return;
                    }
                    case first -> {
                        constraint.mix += (constraint.data.mix - constraint.mix) * alpha;
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                    }
                }
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                if (blend == setup) {
                    constraint.mix = constraint.data.mix + (frames[frames.length + PREV_MIX] - constraint.data.mix) * alpha;
                    if (direction == out) {
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                    } else {
                        constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                        constraint.compress = frames[frames.length + PREV_COMPRESS] != 0;
                        constraint.stretch = frames[frames.length + PREV_STRETCH] != 0;
                    }
                } else {
                    constraint.mix += (frames[frames.length + PREV_MIX] - constraint.mix) * alpha;
                    if (direction == in) {
                        constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                        constraint.compress = frames[frames.length + PREV_COMPRESS] != 0;
                        constraint.stretch = frames[frames.length + PREV_STRETCH] != 0;
                    }
                }
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float mix = frames[frame + PREV_MIX];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            if (blend == setup) {
                constraint.mix = constraint.data.mix + (mix + (frames[frame + MIX] - mix) * percent - constraint.data.mix) * alpha;
                if (direction == out) {
                    constraint.bendDirection = constraint.data.bendDirection;
                    constraint.compress = constraint.data.compress;
                    constraint.stretch = constraint.data.stretch;
                } else {
                    constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
                    constraint.compress = frames[frame + PREV_COMPRESS] != 0;
                    constraint.stretch = frames[frame + PREV_STRETCH] != 0;
                }
            } else {
                constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha;
                if (direction == in) {
                    constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
                    constraint.compress = frames[frame + PREV_COMPRESS] != 0;
                    constraint.stretch = frames[frame + PREV_STRETCH] != 0;
                }
            }
        }
    }

    
    static public class TransformConstraintTimeline extends CurveTimeline {
        static public final int ENTRIES = 5;
        static private final int PREV_TIME = -5, PREV_ROTATE = -4, PREV_TRANSLATE = -3, PREV_SCALE = -2, PREV_SHEAR = -1;
        static private final int ROTATE = 1, TRANSLATE = 2, SCALE = 3, SHEAR = 4;
        private final float[] frames;
        int transformConstraintIndex;

        public TransformConstraintTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.transformConstraint.ordinal() << 24) + transformConstraintIndex;
        }

        
        public int getTransformConstraintIndex() {
            return transformConstraintIndex;
        }

        public void setTransformConstraintIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.transformConstraintIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float rotateMix, float translateMix, float scaleMix, float shearMix) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + ROTATE] = rotateMix;
            frames[frameIndex + TRANSLATE] = translateMix;
            frames[frameIndex + SCALE] = scaleMix;
            frames[frameIndex + SHEAR] = shearMix;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                TransformConstraintData data = constraint.data;
                switch (blend) {
                    case setup -> {
                        constraint.rotateMix = data.rotateMix;
                        constraint.translateMix = data.translateMix;
                        constraint.scaleMix = data.scaleMix;
                        constraint.shearMix = data.shearMix;
                        return;
                    }
                    case first -> {
                        constraint.rotateMix += (data.rotateMix - constraint.rotateMix) * alpha;
                        constraint.translateMix += (data.translateMix - constraint.translateMix) * alpha;
                        constraint.scaleMix += (data.scaleMix - constraint.scaleMix) * alpha;
                        constraint.shearMix += (data.shearMix - constraint.shearMix) * alpha;
                    }
                }
                return;
            }

            float rotate, translate, scale, shear;
            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                rotate = frames[i + PREV_ROTATE];
                translate = frames[i + PREV_TRANSLATE];
                scale = frames[i + PREV_SCALE];
                shear = frames[i + PREV_SHEAR];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                rotate = frames[frame + PREV_ROTATE];
                translate = frames[frame + PREV_TRANSLATE];
                scale = frames[frame + PREV_SCALE];
                shear = frames[frame + PREV_SHEAR];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                rotate += (frames[frame + ROTATE] - rotate) * percent;
                translate += (frames[frame + TRANSLATE] - translate) * percent;
                scale += (frames[frame + SCALE] - scale) * percent;
                shear += (frames[frame + SHEAR] - shear) * percent;
            }
            if (blend == setup) {
                TransformConstraintData data = constraint.data;
                constraint.rotateMix = data.rotateMix + (rotate - data.rotateMix) * alpha;
                constraint.translateMix = data.translateMix + (translate - data.translateMix) * alpha;
                constraint.scaleMix = data.scaleMix + (scale - data.scaleMix) * alpha;
                constraint.shearMix = data.shearMix + (shear - data.shearMix) * alpha;
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha;
                constraint.translateMix += (translate - constraint.translateMix) * alpha;
                constraint.scaleMix += (scale - constraint.scaleMix) * alpha;
                constraint.shearMix += (shear - constraint.shearMix) * alpha;
            }
        }
    }

    
    static public class PathConstraintPositionTimeline extends CurveTimeline {
        static public final int ENTRIES = 2;
        static final int PREV_TIME = -2, PREV_VALUE = -1;
        static final int VALUE = 1;
        final float[] frames;
        int pathConstraintIndex;

        public PathConstraintPositionTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.pathConstraintPosition.ordinal() << 24) + pathConstraintIndex;
        }

        
        public int getPathConstraintIndex() {
            return pathConstraintIndex;
        }

        public void setPathConstraintIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.pathConstraintIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float position) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + VALUE] = position;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
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

            float position;
            if (time >= frames[frames.length - ENTRIES])
                position = frames[frames.length + PREV_VALUE];
            else {

                int frame = binarySearch(frames, time, ENTRIES);
                position = frames[frame + PREV_VALUE];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                position += (frames[frame + VALUE] - position) * percent;
            }
            if (blend == setup)
                constraint.position = constraint.data.position + (position - constraint.data.position) * alpha;
            else
                constraint.position += (position - constraint.position) * alpha;
        }
    }

    
    static public class PathConstraintSpacingTimeline extends PathConstraintPositionTimeline {
        public PathConstraintSpacingTimeline(int frameCount) {
            super(frameCount);
        }

        public int getPropertyId() {
            return (TimelineType.pathConstraintSpacing.ordinal() << 24) + pathConstraintIndex;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
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

            float spacing;
            if (time >= frames[frames.length - ENTRIES])
                spacing = frames[frames.length + PREV_VALUE];
            else {

                int frame = binarySearch(frames, time, ENTRIES);
                spacing = frames[frame + PREV_VALUE];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                spacing += (frames[frame + VALUE] - spacing) * percent;
            }

            if (blend == setup)
                constraint.spacing = constraint.data.spacing + (spacing - constraint.data.spacing) * alpha;
            else
                constraint.spacing += (spacing - constraint.spacing) * alpha;
        }
    }

    
    static public class PathConstraintMixTimeline extends CurveTimeline {
        static public final int ENTRIES = 3;
        static private final int PREV_TIME = -3, PREV_ROTATE = -2, PREV_TRANSLATE = -1;
        static private final int ROTATE = 1, TRANSLATE = 2;
        private final float[] frames;
        int pathConstraintIndex;

        public PathConstraintMixTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
        }

        public int getPropertyId() {
            return (TimelineType.pathConstraintMix.ordinal() << 24) + pathConstraintIndex;
        }

        
        public int getPathConstraintIndex() {
            return pathConstraintIndex;
        }

        public void setPathConstraintIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.pathConstraintIndex = index;
        }

        
        public float[] getFrames() {
            return frames;
        }

        
        public void setFrame(int frameIndex, float time, float rotateMix, float translateMix) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + ROTATE] = rotateMix;
            frames[frameIndex + TRANSLATE] = translateMix;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.rotateMix = constraint.data.rotateMix;
                        constraint.translateMix = constraint.data.translateMix;
                        return;
                    }
                    case first -> {
                        constraint.rotateMix += (constraint.data.rotateMix - constraint.rotateMix) * alpha;
                        constraint.translateMix += (constraint.data.translateMix - constraint.translateMix) * alpha;
                    }
                }
                return;
            }

            float rotate, translate;
            if (time >= frames[frames.length - ENTRIES]) {
                rotate = frames[frames.length + PREV_ROTATE];
                translate = frames[frames.length + PREV_TRANSLATE];
            } else {

                int frame = binarySearch(frames, time, ENTRIES);
                rotate = frames[frame + PREV_ROTATE];
                translate = frames[frame + PREV_TRANSLATE];
                float frameTime = frames[frame];
                float percent = getCurvePercent(frame / ENTRIES - 1,
                        1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

                rotate += (frames[frame + ROTATE] - rotate) * percent;
                translate += (frames[frame + TRANSLATE] - translate) * percent;
            }

            if (blend == setup) {
                constraint.rotateMix = constraint.data.rotateMix + (rotate - constraint.data.rotateMix) * alpha;
                constraint.translateMix = constraint.data.translateMix + (translate - constraint.data.translateMix) * alpha;
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha;
                constraint.translateMix += (translate - constraint.translateMix) * alpha;
            }
        }
    }
}
