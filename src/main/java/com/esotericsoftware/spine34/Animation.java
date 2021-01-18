package com.esotericsoftware.spine34;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine34.attachments.Attachment;
import com.esotericsoftware.spine34.attachments.VertexAttachment;

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
        static public final int ENTRIES = 2;
        static private final int PREV_TIME = -2, PREV_ROTATION = -1;
        static private final int ROTATION = 1;
        final float[] frames;
        int boneIndex;

        public RotateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount << 1];
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

    static public class TranslateTimeline extends CurveTimeline {
        static public final int ENTRIES = 3;
        static final int PREV_TIME = -3, PREV_X = -2, PREV_Y = -1;
        static final int X = 1, Y = 2;
        final float[] frames;
        int boneIndex;

        public TranslateTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

    static public class ColorTimeline extends CurveTimeline {
        static public final int ENTRIES = 5;
        static private final int PREV_TIME = -5, PREV_R = -4, PREV_G = -3, PREV_B = -2, PREV_A = -1;
        static private final int R = 1, G = 2, B = 3, A = 4;
        private final float[] frames;
        int slotIndex;

        public ColorTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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
    }

    static public class DeformTimeline extends CurveTimeline {
        private final float[] frames;
        private final float[][] frameVertices;
        int slotIndex;
        VertexAttachment attachment;

        public DeformTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount];
            frameVertices = new float[frameCount][];
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public void setSlotIndex(int index) {
            if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
            this.slotIndex = index;
        }

        public Attachment getAttachment() {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
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

    static public class IkConstraintTimeline extends CurveTimeline {
        static public final int ENTRIES = 3;
        static private final int PREV_TIME = -3, PREV_MIX = -2, PREV_BEND_DIRECTION = -1;
        static private final int MIX = 1, BEND_DIRECTION = 2;
        private final float[] frames;
        int ikConstraintIndex;

        public IkConstraintTimeline(int frameCount) {
            super(frameCount);
            frames = new float[frameCount * ENTRIES];
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


        public void setFrame(int frameIndex, float time, float mix, int bendDirection) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + MIX] = mix;
            frames[frameIndex + BEND_DIRECTION] = bendDirection;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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


        public void setFrame(int frameIndex, float time, float value) {
            frameIndex *= ENTRIES;
            frames[frameIndex] = time;
            frames[frameIndex + VALUE] = value;
        }

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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

        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
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
