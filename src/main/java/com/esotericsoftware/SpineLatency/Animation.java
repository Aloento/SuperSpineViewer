package com.esotericsoftware.SpineLatency;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.SpineLatency.attachments.Attachment;
import com.esotericsoftware.SpineLatency.attachments.FfdAttachment;

public class Animation {
    final String name;
    private final Array<Timeline> timelines;
    private float duration;

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

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events) {
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        if (loop && duration != 0) {
            time %= duration;
            if (lastTime > 0) lastTime %= duration;
        }
        Array<Timeline> timelines = this.timelines;
        for (int i = 0, n = timelines.size; i < n; i++)
            timelines.get(i).apply(skeleton, lastTime, time, events, 1);
    }

    public void mix(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha) {
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

    public interface Timeline {
        void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha);
    }

    abstract static public class CurveTimeline implements Timeline {
        static public final float LINEAR = 0, STEPPED = 1, BEZIER = 2;
        static private final int BEZIER_SEGMENTS = 10, BEZIER_SIZE = BEZIER_SEGMENTS * 2 - 1;
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
            float subdiv1 = 1f / BEZIER_SEGMENTS, subdiv2 = subdiv1 * subdiv1, subdiv3 = subdiv2 * subdiv1;
            float pre1 = 3 * subdiv1, pre2 = 3 * subdiv2, pre4 = 6 * subdiv2, pre5 = 6 * subdiv3;
            float tmp1x = -cx1 * 2 + cx2, tmp1y = -cy1 * 2 + cy2, tmp2x = (cx1 - cx2) * 3 + 1, tmp2y = (cy1 - cy2) * 3 + 1;
            float dfx = cx1 * pre1 + tmp1x * pre2 + tmp2x * subdiv3, dfy = cy1 * pre1 + tmp1y * pre2 + tmp2y * subdiv3;
            float ddfx = tmp1x * pre4 + tmp2x * pre5, ddfy = tmp1y * pre4 + tmp2y * pre5;
            float dddfx = tmp2x * pre5, dddfy = tmp2y * pre5;
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
            float y = curves[i - 1];
            return y + (1 - y) * (percent - x) / (1 - x);
        }
    }

    static public class RotateTimeline extends CurveTimeline {
        static final int PREV_TIME = -2;
        static final int VALUE = 1;
        final float[] frames;
        int boneIndex;

        public RotateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount << 1];
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void setBoneIndex(int boneIndex) {
            this.boneIndex = boneIndex;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float angle) {
            frameIndex *= 2;
            frames[frameIndex] = time;
            frames[frameIndex + 1] = angle;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - 2]) {
                float amount = bone.data.rotation + frames[frames.length - 1] - bone.rotation;
                while (amount > 180)
                    amount -= 360;
                while (amount < -180)
                    amount += 360;
                bone.rotation += amount * alpha;
                return;
            }
            int frame = binarySearch(frames, time, 2);
            float prevFrameValue = frames[frame - 1];
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent((frame >> 1) - 1, percent);
            float amount = frames[frame + VALUE] - prevFrameValue;
            while (amount > 180)
                amount -= 360;
            while (amount < -180)
                amount += 360;
            amount = bone.data.rotation + (prevFrameValue + amount * percent) - bone.rotation;
            while (amount > 180)
                amount -= 360;
            while (amount < -180)
                amount += 360;
            bone.rotation += amount * alpha;
        }
    }

    static public class TranslateTimeline extends CurveTimeline {
        static final int PREV_TIME = -3;
        static final int X = 1;
        static final int Y = 2;
        final float[] frames;
        int boneIndex;

        public TranslateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * 3];
        }

        public int getBoneIndex() {
            return boneIndex;
        }

        public void setBoneIndex(int boneIndex) {
            this.boneIndex = boneIndex;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float x, float y) {
            frameIndex *= 3;
            frames[frameIndex] = time;
            frames[frameIndex + 1] = x;
            frames[frameIndex + 2] = y;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.x += (bone.data.x + frames[frames.length - 2] - bone.x) * alpha;
                bone.y += (bone.data.y + frames[frames.length - 1] - bone.y) * alpha;
                return;
            }
            int frame = binarySearch(frames, time, 3);
            float prevFrameX = frames[frame - 2];
            float prevFrameY = frames[frame - 1];
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent(frame / 3 - 1, percent);
            bone.x += (bone.data.x + prevFrameX + (frames[frame + X] - prevFrameX) * percent - bone.x) * alpha;
            bone.y += (bone.data.y + prevFrameY + (frames[frame + Y] - prevFrameY) * percent - bone.y) * alpha;
        }
    }

    static public class ScaleTimeline extends TranslateTimeline {
        public ScaleTimeline(int frameCount) {
            super(frameCount);
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.scaleX += (bone.data.scaleX * frames[frames.length - 2] - bone.scaleX) * alpha;
                bone.scaleY += (bone.data.scaleY * frames[frames.length - 1] - bone.scaleY) * alpha;
                return;
            }
            int frame = binarySearch(frames, time, 3);
            float prevFrameX = frames[frame - 2];
            float prevFrameY = frames[frame - 1];
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent(frame / 3 - 1, percent);
            bone.scaleX += (bone.data.scaleX * (prevFrameX + (frames[frame + X] - prevFrameX) * percent) - bone.scaleX) * alpha;
            bone.scaleY += (bone.data.scaleY * (prevFrameY + (frames[frame + Y] - prevFrameY) * percent) - bone.scaleY) * alpha;
        }
    }

    static public class ShearTimeline extends TranslateTimeline {
        public ShearTimeline(int frameCount) {
            super(frameCount);
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            Bone bone = skeleton.bones.get(boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.shearX += (bone.data.shearX + frames[frames.length - 2] - bone.shearX) * alpha;
                bone.shearY += (bone.data.shearY + frames[frames.length - 1] - bone.shearY) * alpha;
                return;
            }
            int frame = binarySearch(frames, time, 3);
            float prevFrameX = frames[frame - 2];
            float prevFrameY = frames[frame - 1];
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent(frame / 3 - 1, percent);
            bone.shearX += (bone.data.shearX + (prevFrameX + (frames[frame + X] - prevFrameX) * percent) - bone.shearX) * alpha;
            bone.shearY += (bone.data.shearY + (prevFrameY + (frames[frame + Y] - prevFrameY) * percent) - bone.shearY) * alpha;
        }
    }

    static public class ColorTimeline extends CurveTimeline {
        static private final int PREV_TIME = -5;
        static private final int R = 1;
        static private final int G = 2;
        static private final int B = 3;
        static private final int A = 4;
        private final float[] frames;
        int slotIndex;

        public ColorTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * 5];
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float r, float g, float b, float a) {
            frameIndex *= 5;
            frames[frameIndex] = time;
            frames[frameIndex + 1] = r;
            frames[frameIndex + 2] = g;
            frames[frameIndex + 3] = b;
            frames[frameIndex + 4] = a;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            float r, g, b, a;
            if (time >= frames[frames.length - 5]) {
                int i = frames.length - 1;
                r = frames[i - 3];
                g = frames[i - 2];
                b = frames[i - 1];
                a = frames[i];
            } else {
                int frame = binarySearch(frames, time, 5);
                float frameTime = frames[frame];
                float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
                percent = getCurvePercent(frame / 5 - 1, percent);
                r = frames[frame - 4];
                g = frames[frame - 3];
                b = frames[frame - 2];
                a = frames[frame - 1];
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

    static public class AttachmentTimeline implements Timeline {
        final float[] frames;
        final String[] attachmentNames;
        int slotIndex;

        public AttachmentTimeline(int frameCount) {
            frames = new float[frameCount];
            attachmentNames = new String[frameCount];
        }

        public int getFrameCount() {
            return frames.length;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int slotIndex) {
            this.slotIndex = slotIndex;
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                if (lastTime > time) apply(skeleton, lastTime, Integer.MAX_VALUE, null, 0);
                return;
            } else if (lastTime > time)
                lastTime = -1;
            int frame = (time >= frames[frames.length - 1] ? frames.length : binarySearch(frames, time)) - 1;
            if (frames[frame] < lastTime) return;
            String attachmentName = attachmentNames[frame];
            skeleton.slots.get(slotIndex)
                    .setAttachment(attachmentName == null ? null : skeleton.getAttachment(slotIndex, attachmentName));
        }
    }

    static public class EventTimeline implements Timeline {
        private final float[] frames;
        private final Event[] events;

        public EventTimeline(int frameCount) {
            frames = new float[frameCount];
            events = new Event[frameCount];
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
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
            frames = new float[frameCount];
            drawOrders = new int[frameCount][];
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
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

    static public class FfdTimeline extends CurveTimeline {
        private final float[] frames;
        private final float[][] frameVertices;
        int slotIndex;
        Attachment attachment;

        public FfdTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount];
            frameVertices = new float[frameCount][];
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        public Attachment getAttachment() {
            return attachment;
        }

        public void setAttachment(Attachment attachment) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
            Slot slot = skeleton.slots.get(slotIndex);
            Attachment slotAttachment = slot.getAttachment();
            if (!(slotAttachment instanceof FfdAttachment) || !((FfdAttachment) slotAttachment).applyFFD(attachment))
                return;
            float[] frames = this.frames;
            if (time < frames[0]) return;
            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;
            FloatArray verticesArray = slot.getAttachmentVertices();
            if (verticesArray.size != vertexCount) alpha = 1;
            verticesArray.size = 0;
            verticesArray.ensureCapacity(vertexCount);
            verticesArray.size = vertexCount;
            float[] vertices = verticesArray.items;
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
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame - 1] - frameTime), 0, 1);
            percent = getCurvePercent(frame - 1, percent);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
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

    static public class IkConstraintTimeline extends CurveTimeline {
        static private final int PREV_TIME = -3;
        static private final int PREV_MIX = -2;
        static private final int PREV_BEND_DIRECTION = -1;
        static private final int MIX = 1;
        private final float[] frames;
        int ikConstraintIndex;

        public IkConstraintTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * 3];
        }

        public int getIkConstraintIndex() {
            return ikConstraintIndex;
        }

        public void setIkConstraintIndex(int ikConstraint) {
            this.ikConstraintIndex = ikConstraint;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float mix, int bendDirection) {
            frameIndex *= 3;
            frames[frameIndex] = time;
            frames[frameIndex + 1] = mix;
            frames[frameIndex + 2] = bendDirection;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            IkConstraint constraint = skeleton.ikConstraints.get(ikConstraintIndex);
            if (time >= frames[frames.length - 3]) {
                constraint.mix += (frames[frames.length + PREV_MIX] - constraint.mix) * alpha;
                constraint.bendDirection = (int) frames[frames.length + PREV_BEND_DIRECTION];
                return;
            }
            int frame = binarySearch(frames, time, 3);
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent(frame / 3 - 1, percent);
            float mix = frames[frame + PREV_MIX];
            constraint.mix += (mix + (frames[frame + MIX] - mix) * percent - constraint.mix) * alpha;
            constraint.bendDirection = (int) frames[frame + PREV_BEND_DIRECTION];
        }
    }

    static public class TransformConstraintTimeline extends CurveTimeline {
        static private final int PREV_TIME = -5;
        static private final int PREV_ROTATE_MIX = -4;
        static private final int PREV_TRANSLATE_MIX = -3;
        static private final int PREV_SCALE_MIX = -2;
        static private final int PREV_SHEAR_MIX = -1;
        static private final int ROTATE_MIX = 1;
        static private final int TRANSLATE_MIX = 2;
        static private final int SCALE_MIX = 3;
        static private final int SHEAR_MIX = 4;
        private final float[] frames;
        int transformConstraintIndex;

        public TransformConstraintTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * 5];
        }

        public int getTransformConstraintIndex() {
            return transformConstraintIndex;
        }

        public void setTransformConstraintIndex(int ikConstraint) {
            this.transformConstraintIndex = ikConstraint;
        }

        public float[] getFrames() {
            return frames;
        }

        public void setFrame(int frameIndex, float time, float rotateMix, float translateMix, float scaleMix, float shearMix) {
            frameIndex *= 5;
            frames[frameIndex] = time;
            frames[frameIndex + 1] = rotateMix;
            frames[frameIndex + 2] = translateMix;
            frames[frameIndex + 3] = scaleMix;
            frames[frameIndex + 4] = shearMix;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) return;
            TransformConstraint constraint = skeleton.transformConstraints.get(transformConstraintIndex);
            if (time >= frames[frames.length - 5]) {
                int i = frames.length - 1;
                constraint.rotateMix += (frames[i - 3] - constraint.rotateMix) * alpha;
                constraint.translateMix += (frames[i - 2] - constraint.translateMix) * alpha;
                constraint.scaleMix += (frames[i - 1] - constraint.scaleMix) * alpha;
                constraint.shearMix += (frames[i] - constraint.shearMix) * alpha;
                return;
            }
            int frame = binarySearch(frames, time, 5);
            float frameTime = frames[frame];
            float percent = MathUtils.clamp(1 - (time - frameTime) / (frames[frame + PREV_TIME] - frameTime), 0, 1);
            percent = getCurvePercent(frame / 5 - 1, percent);
            float rotate = frames[frame + PREV_ROTATE_MIX];
            float translate = frames[frame + PREV_TRANSLATE_MIX];
            float scale = frames[frame + PREV_SCALE_MIX];
            float shear = frames[frame + PREV_SHEAR_MIX];
            constraint.rotateMix += (rotate + (frames[frame + ROTATE_MIX] - rotate) * percent - constraint.rotateMix) * alpha;
            constraint.translateMix += (translate + (frames[frame + TRANSLATE_MIX] - translate) * percent - constraint.translateMix)
                    * alpha;
            constraint.scaleMix += (scale + (frames[frame + SCALE_MIX] - scale) * percent - constraint.scaleMix) * alpha;
            constraint.shearMix += (shear + (frames[frame + SHEAR_MIX] - shear) * percent - constraint.shearMix) * alpha;
        }
    }
}
