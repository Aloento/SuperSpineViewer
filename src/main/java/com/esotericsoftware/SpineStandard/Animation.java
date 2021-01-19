package com.esotericsoftware.SpineStandard;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntSet;
import com.esotericsoftware.SpineStandard.attachments.Attachment;
import com.esotericsoftware.SpineStandard.attachments.VertexAttachment;

import java.util.Objects;

import static com.esotericsoftware.SpineStandard.Animation.MixBlend.*;
import static com.esotericsoftware.SpineStandard.Animation.MixDirection.in;
import static com.esotericsoftware.SpineStandard.Animation.MixDirection.out;
import static com.esotericsoftware.SpineStandard.Animation.MixPose.P_setup;
import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

public class Animation {
    final String name;
    Array<Timeline> timelines;
    IntSet timelineIDs;
    float duration;

    public Animation(String name, Array<Timeline> timelines, float duration) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        if (timelines == null) throw new IllegalArgumentException("timelines cannot be null.");
        this.name = name;
        this.duration = duration;
        if (RuntimesLoader.spineVersion > 37) {
            timelineIDs = new IntSet();
            setTimelines(timelines);
        } else this.timelines = timelines;
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

    public Array<Timeline> getTimelines() {
        return timelines;
    }

    public void setTimelines(Array<Timeline> timelines) {
        if (timelines == null) throw new IllegalArgumentException("timelines cannot be null.");
        this.timelines = timelines;
        timelineIDs.clear();
        for (Timeline timeline : timelines)
            timelineIDs.add(timeline.getPropertyId());
    }

    public boolean hasTimeline(int id) {
        return timelineIDs.contains(id);
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

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha,
                      MixPose pose, MixDirection direction) { // Spine36
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }

        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, alpha, pose, direction);
    }

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha,
                      boolean setupPose, boolean mixingOut) { // Spine35
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }

        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, alpha, setupPose, mixingOut);
    }

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events) { // Spine34
        if (skeleton == null)
            throw new IllegalArgumentException("skeleton cannot be null.");

        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }

        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, 1);
    }

    public void mix(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha) { // Spine34
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }

        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, alpha);
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public enum MixPose { // Spine36
        P_setup, current, currentLayered
    }

    public enum MixBlend {
        setup, first, replace, add
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

        void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                   MixDirection direction); // Spine36

        void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                   boolean mixingOut); // Spine35

        void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha); // Spine34

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

        public void setStepped(int frameIndex) {
            curves[frameIndex * BEZIER_SIZE] = STEPPED;
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
                    if (RuntimesLoader.spineVersion > 34) {
                        if (i == start) return curves[i + 1] * percent / x;
                        float prevX = curves[i - 2], prevY = curves[i - 1];
                        return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX);
                    } else {
                        float prevX, prevY;
                        if (i == start) {
                            prevX = 0;
                            prevY = 0;
                        } else {
                            prevX = curves[i - 2];
                            prevY = curves[i - 1];
                        }
                        return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX);
                    }
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
            if (!bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        bone.rotation = bone.data.rotation;
                        return;
                    }
                    case current -> {
                        float r = bone.data.rotation - bone.rotation;
                        r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                        bone.rotation += r * alpha;
                    }
                }
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                if (pose == P_setup)
                    bone.rotation = bone.data.rotation + frames[frames.length + PREV_ROTATION] * alpha;
                else {
                    float r = bone.data.rotation + frames[frames.length + PREV_ROTATION] - bone.rotation;
                    r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                    bone.rotation += r * alpha;
                }
                return;
            }

            int frame = binarySearch(frames, time, ENTRIES);
            float prevRotation = frames[frame + PREV_ROTATION];
            float frameTime = frames[frame];
            float percent = getCurvePercent((frame >> 1) - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            float r = frames[frame + ROTATION] - prevRotation;
            r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
            r = prevRotation + r * percent;
            if (pose == P_setup) {
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                bone.rotation = bone.data.rotation + r * alpha;
            } else {
                r = bone.data.rotation + r - bone.rotation;
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                bone.rotation += r * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) bone.rotation = bone.data.rotation;
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                if (setupPose)
                    bone.rotation = bone.data.rotation + frames[frames.length + PREV_ROTATION] * alpha;
                else {
                    float r = bone.data.rotation + frames[frames.length + PREV_ROTATION] - bone.rotation;
                    r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                    bone.rotation += r * alpha;
                }
                return;
            }

            int frame = binarySearch(frames, time, ENTRIES);
            float prevRotation = frames[frame + PREV_ROTATION];
            float frameTime = frames[frame];
            float percent = getCurvePercent((frame >> 1) - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            float r = frames[frame + ROTATION] - prevRotation;
            r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
            r = prevRotation + r * percent;
            if (setupPose) {
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                bone.rotation = bone.data.rotation + r * alpha;
            } else {
                r = bone.data.rotation + r - bone.rotation;
                r -= (16384 - (int) (16384.499999999996 - r / 360)) * 360;
                bone.rotation += r * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            Bone bone = skeleton.bones.get(boneIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                float amount = bone.data.rotation + frames[frames.length + PREV_ROTATION] - bone.rotation;
                while (amount > 180)
                    amount -= 360;
                while (amount < -180)
                    amount += 360;
                bone.rotation += amount * alpha;
                return;
            }

            int frame = binarySearch(frames, time, ENTRIES);
            float prevRotation = frames[frame + PREV_ROTATION];
            float frameTime = frames[frame];
            float percent = getCurvePercent((frame >> 1) - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            float amount = frames[frame + ROTATION] - prevRotation;
            while (amount > 180)
                amount -= 360;
            while (amount < -180)
                amount += 360;
            amount = bone.data.rotation + (prevRotation + amount * percent) - bone.rotation;
            while (amount > 180)
                amount -= 360;
            while (amount < -180)
                amount += 360;
            bone.rotation += amount * alpha;
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
            if (!bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        bone.x = bone.data.x;
                        bone.y = bone.data.y;
                        return;
                    }
                    case current -> {
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
            if (pose == P_setup) {
                bone.x = bone.data.x + x * alpha;
                bone.y = bone.data.y + y * alpha;
            } else {
                bone.x += (bone.data.x + x - bone.x) * alpha;
                bone.y += (bone.data.y + y - bone.y) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    bone.x = bone.data.x;
                    bone.y = bone.data.y;
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
            if (setupPose) {
                bone.x = bone.data.x + x * alpha;
                bone.y = bone.data.y + y * alpha;
            } else {
                bone.x += (bone.data.x + x - bone.x) * alpha;
                bone.y += (bone.data.y + y - bone.y) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            Bone bone = skeleton.bones.get(boneIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                bone.x += (bone.data.x + frames[frames.length + PREV_X] - bone.x) * alpha;
                bone.y += (bone.data.y + frames[frames.length + PREV_Y] - bone.y) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float prevX = frames[frame + PREV_X];
            float prevY = frames[frame + PREV_Y];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            bone.x += (bone.data.x + prevX + (frames[frame + X] - prevX) * percent - bone.x) * alpha;
            bone.y += (bone.data.y + prevY + (frames[frame + Y] - prevY) * percent - bone.y) * alpha;
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
            if (!bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36

            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        bone.scaleX = bone.data.scaleX;
                        bone.scaleY = bone.data.scaleY;
                        return;
                    }
                    case current -> {
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
                bone.scaleX = x;
                bone.scaleY = y;
            } else {
                float bx, by;
                if (pose == P_setup) {
                    bx = bone.data.scaleX;
                    by = bone.data.scaleY;
                } else {
                    bx = bone.scaleX;
                    by = bone.scaleY;
                }

                if (direction == out) {
                    x = Math.abs(x) * Math.signum(bx);
                    y = Math.abs(y) * Math.signum(by);
                } else {
                    bx = Math.abs(bx) * Math.signum(x);
                    by = Math.abs(by) * Math.signum(y);
                }
                bone.scaleX = bx + (x - bx) * alpha;
                bone.scaleY = by + (y - by) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    bone.scaleX = bone.data.scaleX;
                    bone.scaleY = bone.data.scaleY;
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
                bone.scaleX = x;
                bone.scaleY = y;
            } else {
                float bx, by;
                if (setupPose) {
                    bx = bone.data.scaleX;
                    by = bone.data.scaleY;
                } else {
                    bx = bone.scaleX;
                    by = bone.scaleY;
                }

                if (mixingOut) {
                    x = Math.abs(x) * Math.signum(bx);
                    y = Math.abs(y) * Math.signum(by);
                } else {
                    bx = Math.abs(bx) * Math.signum(x);
                    by = Math.abs(by) * Math.signum(y);
                }
                bone.scaleX = bx + (x - bx) * alpha;
                bone.scaleY = by + (y - by) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - ENTRIES]) {
                bone.scaleX += (bone.data.scaleX * frames[frames.length + PREV_X] - bone.scaleX) * alpha;
                bone.scaleY += (bone.data.scaleY * frames[frames.length + PREV_Y] - bone.scaleY) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float prevX = frames[frame + PREV_X];
            float prevY = frames[frame + PREV_Y];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            bone.scaleX += (bone.data.scaleX * (prevX + (frames[frame + X] - prevX) * percent) - bone.scaleX) * alpha;
            bone.scaleY += (bone.data.scaleY * (prevY + (frames[frame + Y] - prevY) * percent) - bone.scaleY) * alpha;
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
            if (!bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        bone.shearX = bone.data.shearX;
                        bone.shearY = bone.data.shearY;
                        return;
                    }
                    case current -> {
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
            if (pose == P_setup) {
                bone.shearX = bone.data.shearX + x * alpha;
                bone.shearY = bone.data.shearY + y * alpha;
            } else {
                bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha;
                bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Bone bone = skeleton.bones.get(boneIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    bone.shearX = bone.data.shearX;
                    bone.shearY = bone.data.shearY;
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
            if (setupPose) {
                bone.shearX = bone.data.shearX + x * alpha;
                bone.shearY = bone.data.shearY + y * alpha;
            } else {
                bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha;
                bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - ENTRIES]) {
                bone.shearX += (bone.data.shearX + frames[frames.length + PREV_X] - bone.shearX) * alpha;
                bone.shearY += (bone.data.shearY + frames[frames.length + PREV_Y] - bone.shearY) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float prevX = frames[frame + PREV_X];
            float prevY = frames[frame + PREV_Y];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            bone.shearX += (bone.data.shearX + (prevX + (frames[frame + X] - prevX) * percent) - bone.shearX) * alpha;
            bone.shearY += (bone.data.shearY + (prevY + (frames[frame + Y] - prevY) * percent) - bone.shearY) * alpha;
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
            if (!slot.bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Slot slot = skeleton.slots.get(slotIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        slot.color.set(slot.data.color);
                        return;
                    }
                    case current -> {
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
                if (pose == P_setup) color.set(slot.data.color);
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Slot slot = skeleton.slots.get(slotIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) slot.color.set(slot.data.color);
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
                if (setupPose) color.set(slot.data.color);
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

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
            Color color = skeleton.slots.get(slotIndex).color;
            if (alpha < 1)
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            else
                color.set(r, g, b, a);
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
            if (!slot.bone.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Slot slot = skeleton.slots.get(slotIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        slot.color.set(slot.data.color);
                        slot.darkColor.set(slot.data.darkColor);
                        return;
                    }
                    case current -> {
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
                if (pose == P_setup) {
                    light.set(slot.data.color);
                    dark.set(slot.data.darkColor);
                }
                light.add((r - light.r) * alpha, (g - light.g) * alpha, (b - light.b) * alpha, (a - light.a) * alpha);
                dark.add((r2 - dark.r) * alpha, (g2 - dark.g) * alpha, (b2 - dark.b) * alpha, 0);
            }
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34

        }
    }

    static public class AttachmentTimeline implements SlotTimeline {
        final float[] frames;
        final String[] attachmentNames;
        int slotIndex;

        public AttachmentTimeline(int frameCount) {
            if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be > 0: " + frameCount);
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
            if (RuntimesLoader.spineVersion == 38) {
                if (!slot.bone.active) return;
                if (direction == out) {
                    if (blend == setup)
                        setAttachment(skeleton, slot, slot.data.attachmentName);
                    return;
                }
            } else if (RuntimesLoader.spineVersion == 37) {
                if (direction == out && blend == setup) {
                    String attachmentName = slot.data.attachmentName;
                    slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                    return;
                }
            }
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (RuntimesLoader.spineVersion == 38) {
                    if (blend == setup || blend == first) setAttachment(skeleton, slot, slot.data.attachmentName);
                    return;
                } else if (RuntimesLoader.spineVersion == 37) {
                    if (blend == setup || blend == first) {
                        String attachmentName = slot.data.attachmentName;
                        slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                    }
                    return;
                }
            }
            int frameIndex;
            if (time >= frames[frames.length - 1])
                frameIndex = frames.length - 1;
            else
                frameIndex = binarySearch(frames, time) - 1;
            if (RuntimesLoader.spineVersion == 38) {
                setAttachment(skeleton, slot, attachmentNames[frameIndex]);
            } else if (RuntimesLoader.spineVersion == 37) {
                String attachmentName = attachmentNames[frameIndex];
                slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Slot slot = skeleton.slots.get(slotIndex);
            if (direction == out && pose == P_setup) {
                String attachmentName = slot.data.attachmentName;
                slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (pose == P_setup) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Slot slot = skeleton.slots.get(slotIndex);
            if (mixingOut && setupPose) {
                String attachmentName = slot.data.attachmentName;
                slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            int frameIndex;
            if (time >= frames[frames.length - 1])
                frameIndex = frames.length - 1;
            else
                frameIndex = binarySearch(frames, time, 1) - 1;

            String attachmentName = attachmentNames[frameIndex];
            skeleton.slots.get(slotIndex)
                    .setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
        }

        private void setAttachment(Skeleton skeleton, Slot slot, String attachmentName) {
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
            return switch (RuntimesLoader.spineVersion) {
                case 38, 37, 36 -> (TimelineType.deform.ordinal() << 27) + attachment.getId() + slotIndex;
                case 35 -> (TimelineType.deform.ordinal() << 24) + slotIndex;
                default -> throw new IllegalStateException("Unexpected value: " + RuntimesLoader.spineVersion);
            };
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
            FloatArray deformArray = null;
            FloatArray verticesArray = null;
            if (RuntimesLoader.spineVersion == 38) {
                if (!slot.bone.active) return;
                if (!(slotAttachment instanceof VertexAttachment)
                        || ((VertexAttachment) slotAttachment).getDeformAttachment() != attachment) return;
                deformArray = slot.getDeform();
                if (deformArray.size == 0) blend = setup;
            } else if (RuntimesLoader.spineVersion == 37) {
                if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment) slotAttachment).applyDeform(attachment))
                    return;
                verticesArray = slot.getAttachmentVertices();
                if (verticesArray.size == 0) blend = setup;
            }
            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;
            float[] frames = this.frames;
            float[] deform = null;
            float[] vertices = null;
            if (RuntimesLoader.spineVersion == 38) {
                if (time < frames[0]) {
                    VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                    switch (blend) {
                        case setup -> {
                            Objects.requireNonNull(deformArray).clear();
                            return;
                        }
                        case first -> {
                            if (alpha == 1) {
                                Objects.requireNonNull(deformArray).clear();
                                return;
                            }
                            deform = Objects.requireNonNull(deformArray).setSize(vertexCount);
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
                deform = Objects.requireNonNull(deformArray).setSize(vertexCount);
                if (time >= frames[frames.length - 1]) {
                    float[] lastVertices = frameVertices[frames.length - 1];
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
            } else if (RuntimesLoader.spineVersion == 37) {
                if (time < frames[0]) {
                    VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                    switch (blend) {
                        case setup -> {
                            Objects.requireNonNull(verticesArray).clear();
                            return;
                        }
                        case first -> {
                            if (alpha == 1) {
                                Objects.requireNonNull(verticesArray).clear();
                                return;
                            }
                            vertices = Objects.requireNonNull(verticesArray).setSize(vertexCount);
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
                vertices = Objects.requireNonNull(verticesArray).setSize(vertexCount);
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
            }
            int frame = binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime));
            if (RuntimesLoader.spineVersion == 38) {
                if (alpha == 1) {
                    if (blend == add) {
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(deform)[i] += prev + (nextVertices[i] - prev) * percent - setupVertices[i];
                            }
                        } else {
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(deform)[i] += prev + (nextVertices[i] - prev) * percent;
                            }
                        }
                    } else {
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            Objects.requireNonNull(deform)[i] = prev + (nextVertices[i] - prev) * percent;
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
                                    Objects.requireNonNull(deform)[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha;
                                }
                            } else {
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(deform)[i] = (prev + (nextVertices[i] - prev) * percent) * alpha;
                                }
                            }
                            break;
                        }
                        case first:
                        case replace:
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(deform)[i] += (prev + (nextVertices[i] - prev) * percent - deform[i]) * alpha;
                            }
                            break;
                        case add:
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {
                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(deform)[i] += (prev + (nextVertices[i] - prev) * percent - setupVertices[i]) * alpha;
                                }
                            } else {
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(deform)[i] += (prev + (nextVertices[i] - prev) * percent) * alpha;
                                }
                            }
                    }
                }
            } else if (RuntimesLoader.spineVersion == 37) {
                if (alpha == 1) {
                    if (blend == add) {
                        VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                        if (vertexAttachment.getBones() == null) {
                            float[] setupVertices = vertexAttachment.getVertices();
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(vertices)[i] += prev + (nextVertices[i] - prev) * percent - setupVertices[i];
                            }
                        } else {
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(vertices)[i] += prev + (nextVertices[i] - prev) * percent;
                            }
                        }
                    } else {
                        for (int i = 0; i < vertexCount; i++) {
                            float prev = prevVertices[i];
                            Objects.requireNonNull(vertices)[i] = prev + (nextVertices[i] - prev) * percent;
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
                                    Objects.requireNonNull(vertices)[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha;
                                }
                            } else {
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(vertices)[i] = (prev + (nextVertices[i] - prev) * percent) * alpha;
                                }
                            }
                            break;
                        }
                        case first:
                        case replace:
                            for (int i = 0; i < vertexCount; i++) {
                                float prev = prevVertices[i];
                                Objects.requireNonNull(vertices)[i] += (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                            }
                            break;
                        case add:
                            VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                            if (vertexAttachment.getBones() == null) {
                                float[] setupVertices = vertexAttachment.getVertices();
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(vertices)[i] += (prev + (nextVertices[i] - prev) * percent - setupVertices[i]) * alpha;
                                }
                            } else {
                                for (int i = 0; i < vertexCount; i++) {
                                    float prev = prevVertices[i];
                                    Objects.requireNonNull(vertices)[i] += (prev + (nextVertices[i] - prev) * percent) * alpha;
                                }
                            }
                    }
                }
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Slot slot = skeleton.slots.get(slotIndex);
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment) slotAttachment).applyDeform(attachment))
                return;

            FloatArray verticesArray = slot.getAttachmentVertices();
            if (verticesArray.size == 0) alpha = 1;

            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;

            float[] frames = this.frames;
            if (time < frames[0]) {
                VertexAttachment vertexAttachment = (VertexAttachment) slotAttachment;
                switch (pose) {
                    case P_setup -> {
                        verticesArray.clear();
                        return;
                    }
                    case current -> {
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

                    System.arraycopy(lastVertices, 0, vertices, 0, vertexCount);
                } else if (pose == P_setup) {
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
                } else {

                    for (int i = 0; i < vertexCount; i++)
                        vertices[i] += (lastVertices[i] - vertices[i]) * alpha;
                }
                return;
            }


            int frame = binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime));

            if (alpha == 1) {

                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] = prev + (nextVertices[i] - prev) * percent;
                }
            } else if (pose == P_setup) {
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
            } else {

                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] += (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                }
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Slot slot = skeleton.slots.get(slotIndex);
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment) slotAttachment).applyDeform(attachment))
                return;

            FloatArray verticesArray = slot.getAttachmentVertices();
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) verticesArray.size = 0;
                return;
            }

            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;
            if (verticesArray.size != vertexCount) alpha = 1;
            float[] vertices = verticesArray.setSize(vertexCount);

            if (time >= frames[frames.length - 1]) {
                float[] lastVertices = frameVertices[frames.length - 1];
                if (alpha == 1) {

                    System.arraycopy(lastVertices, 0, vertices, 0, vertexCount);
                } else if (setupPose) {
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
                } else {

                    for (int i = 0; i < vertexCount; i++)
                        vertices[i] += (lastVertices[i] - vertices[i]) * alpha;
                }
                return;
            }

            int frame = binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime));

            if (alpha == 1) {

                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] = prev + (nextVertices[i] - prev) * percent;
                }
            } else if (setupPose) {
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
            } else {

                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] += (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                }
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) { // Spine34
            Slot slot = skeleton.slots.get(slotIndex);
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment) slotAttachment).applyDeform(attachment))
                return;

            float[] frames = this.frames;
            if (time < frames[0]) return;

            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;

            FloatArray verticesArray = slot.getAttachmentVertices();
            if (verticesArray.size != vertexCount) alpha = 1;
            float[] vertices = verticesArray.setSize(vertexCount);

            if (time >= frames[frames.length - 1]) {
                float[] lastVertices = frameVertices[frames.length - 1];
                if (alpha < 1) {
                    for (int i = 0; i < vertexCount; i++)
                        vertices[i] += (lastVertices[i] - vertices[i]) * alpha;
                } else
                    System.arraycopy(lastVertices, 0, vertices, 0, vertexCount);
                return;
            }


            int frame = binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame - 1, 1 - (time - frameTime) / (frames[frame - 1] - frameTime));

            if (alpha < 1) {
                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] += (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                }
            } else {
                for (int i = 0; i < vertexCount; i++) {
                    float prev = prevVertices[i];
                    vertices[i] = prev + (nextVertices[i] - prev) * percent;
                }
            }
        }
    }

    static public class EventTimeline implements Timeline {
        private final float[] frames;
        private final Event[] events;

        public EventTimeline(int frameCount) {
            if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be > 0: " + frameCount);
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            if (firedEvents == null) return;
            float[] frames = this.frames;
            int frameCount = frames.length;

            if (lastTime > time) {
                apply(skeleton, lastTime, Integer.MAX_VALUE, firedEvents, alpha, pose, direction);
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            if (firedEvents == null) return;
            float[] frames = this.frames;
            int frameCount = frames.length;

            if (lastTime > time) {
                apply(skeleton, lastTime, Integer.MAX_VALUE, firedEvents, alpha, setupPose, mixingOut);
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) { // Spine34
            if (firedEvents == null) return;
            float[] frames = this.frames;
            int frameCount = frames.length;

            if (lastTime > time) {
                apply(skeleton, lastTime, Integer.MAX_VALUE, firedEvents, alpha);
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
            if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be > 0: " + frameCount);
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

        public void setFrame(int frameIndex, float time, int[] drawOrder) {
            frames[frameIndex] = time;
            drawOrders[frameIndex] = drawOrder;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
            if (RuntimesLoader.spineVersion == 38) {
                if (direction == out) {
                    if (blend == setup) arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                    return;
                }
            } else if (RuntimesLoader.spineVersion == 37) {
                if (direction == out && blend == setup) {
                    System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                    return;
                }
            }
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (RuntimesLoader.spineVersion == 38) {
                    if (blend == setup || blend == first) arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                    return;
                } else if (RuntimesLoader.spineVersion == 37) {
                    if (blend == setup || blend == first)
                        System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                    return;
                }
            }
            int frame;
            if (time >= frames[frames.length - 1])
                frame = frames.length - 1;
            else
                frame = binarySearch(frames, time) - 1;
            int[] drawOrderToSetupIndex = drawOrders[frame];
            if (drawOrderToSetupIndex == null) {
                if (RuntimesLoader.spineVersion == 38)
                    arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                else if (RuntimesLoader.spineVersion == 37)
                    System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
            } else {
                for (int i = 0, n = drawOrderToSetupIndex.length; i < n; i++)
                    drawOrder.set(i, slots.get(drawOrderToSetupIndex[i]));
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
            if (direction == out && pose == P_setup) {
                System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (pose == P_setup) System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
            if (mixingOut && setupPose) {
                System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
                return;
            }

            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            int frame;
            if (time >= frames[frames.length - 1])
                frame = frames.length - 1;
            else
                frame = binarySearch(frames, time) - 1;

            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
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
        static public int ENTRIES;
        static private int PREV_TIME, PREV_MIX, PREV_SOFTNESS, PREV_BEND_DIRECTION, PREV_COMPRESS, PREV_STRETCH;
        static private int MIX, SOFTNESS, BEND_DIRECTION, COMPRESS, STRETCH;
        private final float[] frames;
        int ikConstraintIndex;

        public IkConstraintTimeline(int frameCount) {
            super(frameCount);
            switch (RuntimesLoader.spineVersion) {
                case 38 -> {
                    ENTRIES = 6;
                    PREV_TIME = -6;
                    PREV_MIX = -5;
                    PREV_SOFTNESS = -4;
                    PREV_BEND_DIRECTION = -3;
                    PREV_COMPRESS = -2;
                    PREV_STRETCH = -1;
                    MIX = 1;
                    SOFTNESS = 2;
                    BEND_DIRECTION = 3;
                    COMPRESS = 4;
                    STRETCH = 5;
                }
                case 37 -> {
                    ENTRIES = 5;
                    PREV_TIME = -5;
                    PREV_MIX = -4;
                    PREV_BEND_DIRECTION = -3;
                    PREV_COMPRESS = -2;
                    PREV_STRETCH = -1;
                    MIX = 1;
                    BEND_DIRECTION = 2;
                    COMPRESS = 3;
                    STRETCH = 4;
                }
                case 36, 35, 34 -> {
                    ENTRIES = 3;
                    PREV_TIME = -3;
                    PREV_MIX = -2;
                    PREV_BEND_DIRECTION = -1;
                    MIX = 1;
                    BEND_DIRECTION = 2;
                }
            }
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

        public void setFrame(int frameIndex, float time, float mix, float softness, int bendDirection, boolean compress,
                             boolean stretch) { // Spine38
            this.setFrame(frameIndex, time, mix, bendDirection, compress, stretch);
            frames[frameIndex + SOFTNESS] = softness;
        }

        public void setFrame(int frameIndex, float time, float mix, int bendDirection, boolean compress, boolean stretch) { // Spine 37
            this.setFrame(frameIndex, time, mix, bendDirection);
            frames[frameIndex + COMPRESS] = compress ? 1 : 0;
            frames[frameIndex + STRETCH] = stretch ? 1 : 0;
        }

        public void setFrame(int frameIndex, float time, float mix, int bendDirection) { // Spine36/5/4
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + MIX] = mix;
            frames[frameIndex + BEND_DIRECTION] = bendDirection;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixBlend blend,
                          MixDirection direction) {
            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            if (!constraint.active && RuntimesLoader.spineVersion == 38) return;
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (blend) {
                    case setup -> {
                        constraint.mix = constraint.data.mix;
                        if (RuntimesLoader.spineVersion == 38)
                            constraint.softness = constraint.data.softness;
                        constraint.bendDirection = constraint.data.bendDirection;
                        constraint.compress = constraint.data.compress;
                        constraint.stretch = constraint.data.stretch;
                        return;
                    }
                    case first -> {
                        constraint.mix += (constraint.data.mix - constraint.mix) * alpha;
                        if (RuntimesLoader.spineVersion == 38)
                            constraint.softness += (constraint.data.softness - constraint.softness) * alpha;
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
                    if (RuntimesLoader.spineVersion == 38)
                        constraint.softness = constraint.data.softness + (frames[frames.length + PREV_SOFTNESS] - constraint.data.softness) * alpha;
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
                    if (RuntimesLoader.spineVersion == 38)
                        constraint.softness += (frames[frames.length + PREV_SOFTNESS] - constraint.softness) * alpha;
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
            float softness = frames[frame + PREV_SOFTNESS];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));
            if (blend == setup) {
                constraint.mix = constraint.data.mix + (mix + (frames[frame + MIX] - mix) * percent - constraint.data.mix) * alpha;
                if (RuntimesLoader.spineVersion == 38)
                    constraint.softness = constraint.data.softness + (softness + (frames[frame + SOFTNESS] - softness) * percent - constraint.data.softness) * alpha;
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
                if (RuntimesLoader.spineVersion == 38)
                    constraint.softness += (softness + (frames[frame + SOFTNESS] - softness) * percent - constraint.softness) * alpha;
                if (direction == in) {
                    constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
                    constraint.compress = frames[frame + PREV_COMPRESS] != 0;
                    constraint.stretch = frames[frame + PREV_STRETCH] != 0;
                }
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        constraint.mix = constraint.data.mix;
                        constraint.bendDirection = constraint.data.bendDirection;
                        return;
                    }
                    case current -> {
                        constraint.mix += (constraint.data.mix - constraint.mix) * alpha;
                        constraint.bendDirection = constraint.data.bendDirection;
                    }
                }
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                if (pose == P_setup) {
                    constraint.mix = constraint.data.mix + (frames[frames.length + PREV_MIX] - constraint.data.mix) * alpha;
                    constraint.bendDirection = direction == out ? constraint.data.bendDirection
                            : (int) frames[frames.length + PREV_BEND_DIRECTION];
                } else {
                    constraint.mix += (frames[frames.length + PREV_MIX] - constraint.mix) * alpha;
                    if (direction == in) constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                }
                return;
            }

            int frame = binarySearch(frames, time, ENTRIES);
            float mix = frames[frame + PREV_MIX];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            if (pose == P_setup) {
                constraint.mix = constraint.data.mix + (mix + (frames[frame + MIX] - mix) * percent - constraint.data.mix) * alpha;
                constraint.bendDirection = direction == out ? constraint.data.bendDirection
                        : (int) frames[frame + PREV_BEND_DIRECTION];
            } else {
                constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha;
                if (direction == in) constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    constraint.mix = constraint.data.mix;
                    constraint.bendDirection = constraint.data.bendDirection;
                }
                return;
            }

            if (time >= frames[frames.length - ENTRIES]) {
                if (setupPose) {
                    constraint.mix = constraint.data.mix + (frames[frames.length + PREV_MIX] - constraint.data.mix) * alpha;
                    constraint.bendDirection = mixingOut ? constraint.data.bendDirection
                            : (int) frames[frames.length + PREV_BEND_DIRECTION];
                } else {
                    constraint.mix += (frames[frames.length + PREV_MIX] - constraint.mix) * alpha;
                    if (!mixingOut) constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                }
                return;
            }

            int frame = binarySearch(frames, time, ENTRIES);
            float mix = frames[frame + PREV_MIX];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            if (setupPose) {
                constraint.mix = constraint.data.mix + (mix + (frames[frame + MIX] - mix) * percent - constraint.data.mix) * alpha;
                constraint.bendDirection = mixingOut ? constraint.data.bendDirection : (int) frames[frame + PREV_BEND_DIRECTION];
            } else {
                constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha;
                if (!mixingOut) constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                constraint.mix += (frames[frames.length + PREV_MIX] - constraint.mix) * alpha;
                constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float mix = frames[frame + PREV_MIX];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha;
            constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
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
            if (!constraint.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                TransformConstraintData data = constraint.data;
                switch (pose) {
                    case P_setup -> {
                        constraint.rotateMix = data.rotateMix;
                        constraint.translateMix = data.translateMix;
                        constraint.scaleMix = data.scaleMix;
                        constraint.shearMix = data.shearMix;
                        return;
                    }
                    case current -> {
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
            if (pose == P_setup) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    TransformConstraintData data = constraint.data;
                    constraint.rotateMix = data.rotateMix;
                    constraint.translateMix = data.translateMix;
                    constraint.scaleMix = data.scaleMix;
                    constraint.shearMix = data.shearMix;
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
            if (setupPose) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                constraint.rotateMix += (frames[i + PREV_ROTATE] - constraint.rotateMix) * alpha;
                constraint.translateMix += (frames[i + PREV_TRANSLATE] - constraint.translateMix) * alpha;
                constraint.scaleMix += (frames[i + PREV_SCALE] - constraint.scaleMix) * alpha;
                constraint.shearMix += (frames[i + PREV_SHEAR] - constraint.shearMix) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            float rotate = frames[frame + PREV_ROTATE];
            float translate = frames[frame + PREV_TRANSLATE];
            float scale = frames[frame + PREV_SCALE];
            float shear = frames[frame + PREV_SHEAR];
            constraint.rotateMix += (rotate + (frames[frame + ROTATE] - rotate) * percent - constraint.rotateMix) * alpha;
            constraint.translateMix += (translate + (frames[frame + TRANSLATE] - translate) * percent - constraint.translateMix)
                    * alpha;
            constraint.scaleMix += (scale + (frames[frame + SCALE] - scale) * percent - constraint.scaleMix) * alpha;
            constraint.shearMix += (shear + (frames[frame + SHEAR] - shear) * percent - constraint.shearMix) * alpha;
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
            if (!constraint.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        constraint.position = constraint.data.position;
                        return;
                    }
                    case current -> constraint.position += (constraint.data.position - constraint.position) * alpha;
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
            if (pose == P_setup)
                constraint.position = constraint.data.position + (position - constraint.data.position) * alpha;
            else
                constraint.position += (position - constraint.position) * alpha;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) constraint.position = constraint.data.position;
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
            if (setupPose)
                constraint.position = constraint.data.position + (position - constraint.data.position) * alpha;
            else
                constraint.position += (position - constraint.position) * alpha;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                constraint.position += (frames[i + PREV_VALUE] - constraint.position) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float position = frames[frame + PREV_VALUE];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            constraint.position += (position + (frames[frame + VALUE] - position) * percent - constraint.position) * alpha;
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
            if (!constraint.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        constraint.spacing = constraint.data.spacing;
                        return;
                    }
                    case current -> constraint.spacing += (constraint.data.spacing - constraint.spacing) * alpha;
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

            if (pose == P_setup)
                constraint.spacing = constraint.data.spacing + (spacing - constraint.data.spacing) * alpha;
            else
                constraint.spacing += (spacing - constraint.spacing) * alpha;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) constraint.spacing = constraint.data.spacing;
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

            if (setupPose)
                constraint.spacing = constraint.data.spacing + (spacing - constraint.data.spacing) * alpha;
            else
                constraint.spacing += (spacing - constraint.spacing) * alpha;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                constraint.spacing += (frames[i + PREV_VALUE] - constraint.spacing) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float spacing = frames[frame + PREV_VALUE];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            constraint.spacing += (spacing + (frames[frame + VALUE] - spacing) * percent - constraint.spacing) * alpha;
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
            if (!constraint.active && RuntimesLoader.spineVersion == 38) return;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, MixPose pose,
                          MixDirection direction) { // Spine36
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                switch (pose) {
                    case P_setup -> {
                        constraint.rotateMix = constraint.data.rotateMix;
                        constraint.translateMix = constraint.data.translateMix;
                        return;
                    }
                    case current -> {
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

            if (pose == P_setup) {
                constraint.rotateMix = constraint.data.rotateMix + (rotate - constraint.data.rotateMix) * alpha;
                constraint.translateMix = constraint.data.translateMix + (translate - constraint.data.translateMix) * alpha;
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha;
                constraint.translateMix += (translate - constraint.translateMix) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha, boolean setupPose,
                          boolean mixingOut) { // Spine35
            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (setupPose) {
                    constraint.rotateMix = constraint.data.rotateMix;
                    constraint.translateMix = constraint.data.translateMix;
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

            if (setupPose) {
                constraint.rotateMix = constraint.data.rotateMix + (rotate - constraint.data.rotateMix) * alpha;
                constraint.translateMix = constraint.data.translateMix + (translate - constraint.data.translateMix) * alpha;
            } else {
                constraint.rotateMix += (rotate - constraint.rotateMix) * alpha;
                constraint.translateMix += (translate - constraint.translateMix) * alpha;
            }
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) { // Spine34
            float[] frames = this.frames;
            if (time < frames[0]) return;

            PathConstraint constraint = skeleton.pathConstraints.get(pathConstraintIndex);

            if (time >= frames[frames.length - ENTRIES]) {
                int i = frames.length;
                constraint.rotateMix += (frames[i + PREV_ROTATE] - constraint.rotateMix) * alpha;
                constraint.translateMix += (frames[i + PREV_TRANSLATE] - constraint.translateMix) * alpha;
                return;
            }


            int frame = binarySearch(frames, time, ENTRIES);
            float rotate = frames[frame + PREV_ROTATE];
            float translate = frames[frame + PREV_TRANSLATE];
            float frameTime = frames[frame];
            float percent = getCurvePercent(frame / ENTRIES - 1, 1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime));

            constraint.rotateMix += (rotate + (frames[frame + ROTATE] - rotate) * percent - constraint.rotateMix) * alpha;
            constraint.translateMix += (translate + (frames[frame + TRANSLATE] - translate) * percent - constraint.translateMix)
                    * alpha;
        }
    }
}
