package com.esotericsoftware.SpinePreview;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.esotericsoftware.SpinePreview.Animation.*;

import java.lang.StringBuilder;

public class AnimationState {
    static private final Animation emptyAnimation = new Animation("<empty>", new Array<>(0), 0);
    static private final int SUBSEQUENT = 0;
    static private final int FIRST = 1;
    static private final int HOLD_SUBSEQUENT = 2;
    static private final int HOLD_FIRST = 3;
    static private final int HOLD_MIX = 4;
    static private final int SETUP = 1, CURRENT = 2;
    final Array<TrackEntry> tracks = new Array<>();
    final SnapshotArray<AnimationStateListener> listeners = new SnapshotArray<>();
    final Pool<TrackEntry> trackEntryPool = new Pool<>() {
        protected TrackEntry newObject() {
            return new TrackEntry();
        }
    };
    private final Array<Event> events = new Array<>();
    private final EventQueue queue = new EventQueue();
    private final ObjectSet<String> propertyIds = new ObjectSet<>();
    private final AnimationStateData data;
    boolean animationsChanged;
    private float timeScale = 1;
    private int unkeyedState;

    // public AnimationState() {
    // }

    public AnimationState(AnimationStateData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.data = data;
    }

    public void update(float delta) {
        delta *= timeScale;
        Object[] tracks = this.tracks.items;
        for (int i = 0, n = this.tracks.size; i < n; i++) {
            TrackEntry current = (TrackEntry) tracks[i];
            if (current == null) continue;
            current.animationLast = current.nextAnimationLast;
            current.trackLast = current.nextTrackLast;
            float currentDelta = delta * current.timeScale;
            if (current.delay > 0) {
                current.delay -= currentDelta;
                if (current.delay > 0) continue;
                currentDelta = -current.delay;
                current.delay = 0;
            }
            TrackEntry next = current.next;
            if (next != null) {
                float nextTime = current.trackLast - next.delay;
                if (nextTime >= 0) {
                    next.delay = 0;
                    next.trackTime += current.timeScale == 0 ? 0 : (nextTime / current.timeScale + delta) * next.timeScale;
                    current.trackTime += currentDelta;
                    setCurrent(i, next, true);
                    while (next.mixingFrom != null) {
                        next.mixTime += delta;
                        next = next.mixingFrom;
                    }
                    continue;
                }
            } else if (current.trackLast >= current.trackEnd && current.mixingFrom == null) {
                tracks[i] = null;
                queue.end(current);
                disposeNext(current);
                continue;
            }
            if (current.mixingFrom != null && updateMixingFrom(current, delta)) {
                TrackEntry from = current.mixingFrom;
                current.mixingFrom = null;
                if (from != null) from.mixingTo = null;
                while (from != null) {
                    queue.end(from);
                    from = from.mixingFrom;
                }
            }
            current.trackTime += currentDelta;
        }
        queue.drain();
    }

    private boolean updateMixingFrom(TrackEntry to, float delta) {
        TrackEntry from = to.mixingFrom;
        if (from == null) return true;
        boolean finished = updateMixingFrom(from, delta);
        from.animationLast = from.nextAnimationLast;
        from.trackLast = from.nextTrackLast;
        if (to.mixTime > 0 && to.mixTime >= to.mixDuration) {
            if (from.totalAlpha == 0 || to.mixDuration == 0) {
                to.mixingFrom = from.mixingFrom;
                if (from.mixingFrom != null) from.mixingFrom.mixingTo = to;
                to.interruptAlpha = from.interruptAlpha;
                queue.end(from);
            }
            return finished;
        }
        from.trackTime += delta * from.timeScale;
        to.mixTime += delta;
        return false;
    }

    public boolean apply(Skeleton skeleton) {
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        if (animationsChanged) animationsChanged();
        Array<Event> events = this.events;
        boolean applied = false;
        Object[] tracks = this.tracks.items;
        for (int i = 0, n = this.tracks.size; i < n; i++) {
            TrackEntry current = (TrackEntry) tracks[i];
            if (current == null || current.delay > 0) continue;
            applied = true;
            MixBlend blend = i == 0 ? MixBlend.first : current.mixBlend;
            float mix = current.alpha;
            if (current.mixingFrom != null)
                mix *= applyMixingFrom(current, skeleton, blend);
            else if (current.trackTime >= current.trackEnd && current.next == null)
                mix = 0;
            float animationLast = current.animationLast, animationTime = current.getAnimationTime(), applyTime = animationTime;
            Array<Event> applyEvents = events;
            if (current.reverse) {
                applyTime = current.animation.duration - applyTime;
                applyEvents = null;
            }
            int timelineCount = current.animation.timelines.size;
            Object[] timelines = current.animation.timelines.items;
            if ((i == 0 && mix == 1) || blend == MixBlend.add) {
                for (int ii = 0; ii < timelineCount; ii++) {
                    Object timeline = timelines[ii];
                    if (timeline instanceof AttachmentTimeline)
                        applyAttachmentTimeline((AttachmentTimeline) timeline, skeleton, applyTime, blend, true);
                    else
                        ((Timeline) timeline).apply(skeleton, animationLast, applyTime, applyEvents, mix, blend, MixDirection.in);
                }
            } else {
                int[] timelineMode = current.timelineMode.items;
                boolean firstFrame = current.timelinesRotation.size != timelineCount << 1;
                if (firstFrame) current.timelinesRotation.setSize(timelineCount << 1);
                float[] timelinesRotation = current.timelinesRotation.items;
                for (int ii = 0; ii < timelineCount; ii++) {
                    Timeline timeline = (Timeline) timelines[ii];
                    MixBlend timelineBlend = timelineMode[ii] == SUBSEQUENT ? blend : MixBlend.setup;
                    if (timeline instanceof RotateTimeline) {
                        applyRotateTimeline((RotateTimeline) timeline, skeleton, applyTime, mix, timelineBlend, timelinesRotation,
                                ii << 1, firstFrame);
                    } else if (timeline instanceof AttachmentTimeline)
                        applyAttachmentTimeline((AttachmentTimeline) timeline, skeleton, applyTime, blend, true);
                    else
                        timeline.apply(skeleton, animationLast, applyTime, applyEvents, mix, timelineBlend, MixDirection.in);
                }
            }
            queueEvents(current, animationTime);
            events.clear();
            current.nextAnimationLast = animationTime;
            current.nextTrackLast = current.trackTime;
        }
        int setupState = unkeyedState + SETUP;
        Object[] slots = skeleton.slots.items;
        for (int i = 0, n = skeleton.slots.size; i < n; i++) {
            Slot slot = (Slot) slots[i];
            if (slot.attachmentState == setupState) {
                String attachmentName = slot.data.attachmentName;
                slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slot.data.index, attachmentName));
            }
        }
        unkeyedState += 2;
        queue.drain();
        return applied;
    }

    private float applyMixingFrom(TrackEntry to, Skeleton skeleton, MixBlend blend) {
        TrackEntry from = to.mixingFrom;
        if (from.mixingFrom != null) applyMixingFrom(from, skeleton, blend);
        float mix;
        if (to.mixDuration == 0) {
            mix = 1;
            if (blend == MixBlend.first)
                blend = MixBlend.setup;
        } else {
            mix = to.mixTime / to.mixDuration;
            if (mix > 1) mix = 1;
            if (blend != MixBlend.first) blend = from.mixBlend;
        }
        boolean attachments = mix < from.attachmentThreshold, drawOrder = mix < from.drawOrderThreshold;
        int timelineCount = from.animation.timelines.size;
        Object[] timelines = from.animation.timelines.items;
        float alphaHold = from.alpha * to.interruptAlpha, alphaMix = alphaHold * (1 - mix);
        float animationLast = from.animationLast, animationTime = from.getAnimationTime(), applyTime = animationTime;
        Array<Event> events = null;
        if (from.reverse)
            applyTime = from.animation.duration - applyTime;
        else {
            if (mix < from.eventThreshold) events = this.events;
        }
        if (blend == MixBlend.add) {
            for (int i = 0; i < timelineCount; i++)
                ((Timeline) timelines[i]).apply(skeleton, animationLast, applyTime, events, alphaMix, blend, MixDirection.out);
        } else {
            int[] timelineMode = from.timelineMode.items;
            Object[] timelineHoldMix = from.timelineHoldMix.items;
            boolean firstFrame = from.timelinesRotation.size != timelineCount << 1;
            if (firstFrame) from.timelinesRotation.setSize(timelineCount << 1);
            float[] timelinesRotation = from.timelinesRotation.items;
            from.totalAlpha = 0;
            for (int i = 0; i < timelineCount; i++) {
                Timeline timeline = (Timeline) timelines[i];
                MixDirection direction = MixDirection.out;
                MixBlend timelineBlend;
                float alpha;
                switch (timelineMode[i]) {
                    case SUBSEQUENT -> {
                        if (!drawOrder && timeline instanceof DrawOrderTimeline) continue;
                        timelineBlend = blend;
                        alpha = alphaMix;
                    }
                    case FIRST -> {
                        timelineBlend = MixBlend.setup;
                        alpha = alphaMix;
                    }
                    case HOLD_SUBSEQUENT -> {
                        timelineBlend = blend;
                        alpha = alphaHold;
                    }
                    case HOLD_FIRST -> {
                        timelineBlend = MixBlend.setup;
                        alpha = alphaHold;
                    }
                    default -> {
                        timelineBlend = MixBlend.setup;
                        TrackEntry holdMix = (TrackEntry) timelineHoldMix[i];
                        alpha = alphaHold * Math.max(0, 1 - holdMix.mixTime / holdMix.mixDuration);
                    }
                }
                from.totalAlpha += alpha;
                if (timeline instanceof RotateTimeline) {
                    applyRotateTimeline((RotateTimeline) timeline, skeleton, applyTime, alpha, timelineBlend, timelinesRotation, i << 1,
                            firstFrame);
                } else if (timeline instanceof AttachmentTimeline)
                    applyAttachmentTimeline((AttachmentTimeline) timeline, skeleton, applyTime, timelineBlend, attachments);
                else {
                    if (drawOrder && timeline instanceof DrawOrderTimeline && timelineBlend == MixBlend.setup)
                        direction = MixDirection.in;
                    timeline.apply(skeleton, animationLast, applyTime, events, alpha, timelineBlend, direction);
                }
            }
        }
        if (to.mixDuration > 0) queueEvents(from, animationTime);
        this.events.clear();
        from.nextAnimationLast = animationTime;
        from.nextTrackLast = from.trackTime;
        return mix;
    }

    private void applyAttachmentTimeline(AttachmentTimeline timeline, Skeleton skeleton, float time, MixBlend blend,
                                         boolean attachments) {
        Slot slot = skeleton.slots.get(timeline.slotIndex);
        if (!slot.bone.active) return;
        float[] frames = timeline.frames;
        if (time < frames[0]) {
            if (blend == MixBlend.setup || blend == MixBlend.first)
                setAttachment(skeleton, slot, slot.data.attachmentName, attachments);
        } else
            setAttachment(skeleton, slot, timeline.attachmentNames[Animation.search(frames, time)], attachments);
        if (slot.attachmentState <= unkeyedState) slot.attachmentState = unkeyedState + SETUP;
    }

    private void setAttachment(Skeleton skeleton, Slot slot, String attachmentName, boolean attachments) {
        slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slot.data.index, attachmentName));
        if (attachments) slot.attachmentState = unkeyedState + CURRENT;
    }

    private void applyRotateTimeline(RotateTimeline timeline, Skeleton skeleton, float time, float alpha, MixBlend blend,
                                     float[] timelinesRotation, int i, boolean firstFrame) {
        if (firstFrame) timelinesRotation[i] = 0;
        if (alpha == 1) {
            timeline.apply(skeleton, 0, time, null, 1, blend, MixDirection.in);
            return;
        }
        Bone bone = skeleton.bones.get(timeline.boneIndex);
        if (!bone.active) return;
        float[] frames = timeline.frames;
        float r1, r2;
        if (time < frames[0]) {
            switch (blend) {
                case setup:
                    bone.rotation = bone.data.rotation;
                default:
                    return;
                case first:
                    r1 = bone.rotation;
                    r2 = bone.data.rotation;
            }
        } else {
            r1 = blend == MixBlend.setup ? bone.data.rotation : bone.rotation;
            r2 = bone.data.rotation + timeline.getCurveValue(time);
        }
        float total, diff = r2 - r1;
        diff -= (16384 - (int) (16384.499999999996 - diff / 360)) * 360;
        if (diff == 0)
            total = timelinesRotation[i];
        else {
            float lastTotal, lastDiff;
            if (firstFrame) {
                lastTotal = 0;
                lastDiff = diff;
            } else {
                lastTotal = timelinesRotation[i];
                lastDiff = timelinesRotation[i + 1];
            }
            boolean current = diff > 0, dir = lastTotal >= 0;
            if (Math.signum(lastDiff) != Math.signum(diff) && Math.abs(lastDiff) <= 90) {
                if (Math.abs(lastTotal) > 180) lastTotal += 360 * Math.signum(lastTotal);
                dir = current;
            }
            total = diff + lastTotal - lastTotal % 360;
            if (dir != current) total += 360 * Math.signum(lastTotal);
            timelinesRotation[i] = total;
        }
        timelinesRotation[i + 1] = diff;
        bone.rotation = r1 + total * alpha;
    }

    private void queueEvents(TrackEntry entry, float animationTime) {
        float animationStart = entry.animationStart, animationEnd = entry.animationEnd;
        float duration = animationEnd - animationStart;
        float trackLastWrapped = entry.trackLast % duration;
        Object[] events = this.events.items;
        int i = 0, n = this.events.size;
        for (; i < n; i++) {
            Event event = (Event) events[i];
            if (event.time < trackLastWrapped) break;
            if (event.time > animationEnd) continue;
            queue.event(entry, event);
        }
        boolean complete;
        if (entry.loop)
            complete = duration == 0 || trackLastWrapped > entry.trackTime % duration;
        else
            complete = animationTime >= animationEnd && entry.animationLast < animationEnd;
        if (complete) queue.complete(entry);
        for (; i < n; i++) {
            Event event = (Event) events[i];
            if (event.time < animationStart) continue;
            queue.event(entry, event);
        }
    }

    // public void clearTracks() {
    //     boolean oldDrainDisabled = queue.drainDisabled;
    //     queue.drainDisabled = true;
    //     for (int i = 0, n = tracks.size; i < n; i++)
    //         clearTrack(i);
    //     tracks.clear();
    //     queue.drainDisabled = oldDrainDisabled;
    //     queue.drain();
    // }

    // public void clearTrack(int trackIndex) {
    //     if (trackIndex < 0) throw new IllegalArgumentException("trackIndex must be >= 0.");
    //     if (trackIndex >= tracks.size) return;
    //     TrackEntry current = tracks.get(trackIndex);
    //     if (current == null) return;
    //     queue.end(current);
    //     disposeNext(current);
    //     TrackEntry entry = current;
    //     while (true) {
    //         TrackEntry from = entry.mixingFrom;
    //         if (from == null) break;
    //         queue.end(from);
    //         entry.mixingFrom = null;
    //         entry.mixingTo = null;
    //         entry = from;
    //     }
    //     tracks.set(current.trackIndex, null);
    //     queue.drain();
    // }

    // public void clearNext(TrackEntry entry) {
    //     disposeNext(entry.next);
    // }

    private void setCurrent(int index, TrackEntry current, boolean interrupt) {
        TrackEntry from = expandToIndex(index);
        tracks.set(index, current);
        current.previous = null;
        if (from != null) {
            if (interrupt) queue.interrupt(from);
            current.mixingFrom = from;
            from.mixingTo = current;
            current.mixTime = 0;
            if (from.mixingFrom != null && from.mixDuration > 0)
                current.interruptAlpha *= Math.min(1, from.mixTime / from.mixDuration);
            from.timelinesRotation.clear();
        }
        queue.start(current);
    }

    public TrackEntry setAnimation(int trackIndex, String animationName, boolean loop) {
        Animation animation = data.skeletonData.findAnimation(animationName);
        if (animation == null) throw new IllegalArgumentException("Animation not found: " + animationName);
        return setAnimation(trackIndex, animation, loop);
    }

    public TrackEntry setAnimation(int trackIndex, Animation animation, boolean loop) {
        if (trackIndex < 0) throw new IllegalArgumentException("trackIndex must be >= 0.");
        if (animation == null) throw new IllegalArgumentException("animation cannot be null.");
        boolean interrupt = true;
        TrackEntry current = expandToIndex(trackIndex);
        if (current != null) {
            if (current.nextTrackLast == -1) {
                tracks.set(trackIndex, current.mixingFrom);
                queue.interrupt(current);
                queue.end(current);
                disposeNext(current);
                current = current.mixingFrom;
                interrupt = false;
            } else
                disposeNext(current);
        }
        TrackEntry entry = trackEntry(trackIndex, animation, loop, current);
        setCurrent(trackIndex, entry, interrupt);
        queue.drain();
        return entry;
    }

    // public TrackEntry addAnimation(int trackIndex, String animationName, boolean loop, float delay) {
    //     Animation animation = data.skeletonData.findAnimation(animationName);
    //     if (animation == null) throw new IllegalArgumentException("Animation not found: " + animationName);
    //     return addAnimation(trackIndex, animation, loop, delay);
    // }

    // public TrackEntry addAnimation(int trackIndex, Animation animation, boolean loop, float delay) {
    //     if (trackIndex < 0) throw new IllegalArgumentException("trackIndex must be >= 0.");
    //     if (animation == null) throw new IllegalArgumentException("animation cannot be null.");
    //     TrackEntry last = expandToIndex(trackIndex);
    //     if (last != null) {
    //         while (last.next != null)
    //             last = last.next;
    //     }
    //     TrackEntry entry = trackEntry(trackIndex, animation, loop, last);
    //     if (last == null) {
    //         setCurrent(trackIndex, entry, true);
    //         queue.drain();
    //     } else {
    //         last.next = entry;
    //         entry.previous = last;
    //         if (delay <= 0) delay += last.getTrackComplete() - entry.mixDuration;
    //     }
    //     entry.delay = delay;
    //     return entry;
    // }

    public TrackEntry setEmptyAnimation(int trackIndex, float mixDuration) {
        TrackEntry entry = setAnimation(trackIndex, emptyAnimation, false);
        entry.mixDuration = mixDuration;
        entry.trackEnd = mixDuration;
        return entry;
    }

    // public TrackEntry addEmptyAnimation(int trackIndex, float mixDuration, float delay) {
    //     TrackEntry entry = addAnimation(trackIndex, emptyAnimation, false, delay <= 0 ? 1 : delay);
    //     entry.mixDuration = mixDuration;
    //     entry.trackEnd = mixDuration;
    //     if (delay <= 0 && entry.previous != null) entry.delay = entry.previous.getTrackComplete() - entry.mixDuration;
    //     return entry;
    // }

    // public void setEmptyAnimations(float mixDuration) {
    //     boolean oldDrainDisabled = queue.drainDisabled;
    //     queue.drainDisabled = true;
    //     Object[] tracks = this.tracks.items;
    //     for (int i = 0, n = this.tracks.size; i < n; i++) {
    //         TrackEntry current = (TrackEntry) tracks[i];
    //         if (current != null) setEmptyAnimation(current.trackIndex, mixDuration);
    //     }
    //     queue.drainDisabled = oldDrainDisabled;
    //     queue.drain();
    // }

    private TrackEntry expandToIndex(int index) {
        if (index < tracks.size) return tracks.get(index);
        tracks.ensureCapacity(index - tracks.size + 1);
        tracks.size = index + 1;
        return null;
    }

    private TrackEntry trackEntry(int trackIndex, Animation animation, boolean loop, @Null TrackEntry last) {
        TrackEntry entry = trackEntryPool.obtain();
        entry.trackIndex = trackIndex;
        entry.animation = animation;
        entry.loop = loop;
        entry.holdPrevious = false;
        entry.eventThreshold = 0;
        entry.attachmentThreshold = 0;
        entry.drawOrderThreshold = 0;
        entry.animationStart = 0;
        entry.animationEnd = animation.getDuration();
        entry.animationLast = -1;
        entry.nextAnimationLast = -1;
        entry.delay = 0;
        entry.trackTime = 0;
        entry.trackLast = -1;
        entry.nextTrackLast = -1;
        entry.trackEnd = Float.MAX_VALUE;
        entry.timeScale = 1;
        entry.alpha = 1;
        entry.interruptAlpha = 1;
        entry.mixTime = 0;
        entry.mixDuration = last == null ? 0 : data.getMix(last.animation, animation);
        return entry;
    }

    private void disposeNext(TrackEntry entry) {
        TrackEntry next = entry.next;
        while (next != null) {
            queue.dispose(next);
            next = next.next;
        }
        entry.next = null;
    }

    void animationsChanged() {
        animationsChanged = false;
        propertyIds.clear(2048);
        int n = tracks.size;
        Object[] tracks = this.tracks.items;
        for (int i = 0; i < n; i++) {
            TrackEntry entry = (TrackEntry) tracks[i];
            if (entry == null) continue;
            while (entry.mixingFrom != null)
                entry = entry.mixingFrom;
            do {
                if (entry.mixingTo == null || entry.mixBlend != MixBlend.add) computeHold(entry);
                entry = entry.mixingTo;
            } while (entry != null);
        }
    }

    private void computeHold(TrackEntry entry) {
        TrackEntry to = entry.mixingTo;
        Object[] timelines = entry.animation.timelines.items;
        int timelinesCount = entry.animation.timelines.size;
        int[] timelineMode = entry.timelineMode.setSize(timelinesCount);
        entry.timelineHoldMix.clear();
        Object[] timelineHoldMix = entry.timelineHoldMix.setSize(timelinesCount);
        ObjectSet<String> propertyIds = this.propertyIds;
        if (to != null && to.holdPrevious) {
            for (int i = 0; i < timelinesCount; i++)
                timelineMode[i] = propertyIds.addAll(((Timeline) timelines[i]).getPropertyIds()) ? HOLD_FIRST : HOLD_SUBSEQUENT;
            return;
        }
        outer:
        for (int i = 0; i < timelinesCount; i++) {
            Timeline timeline = (Timeline) timelines[i];
            String[] ids = timeline.getPropertyIds();
            if (!propertyIds.addAll(ids))
                timelineMode[i] = SUBSEQUENT;
            else if (to == null || timeline instanceof AttachmentTimeline || timeline instanceof DrawOrderTimeline
                    || timeline instanceof EventTimeline || !to.animation.hasTimeline(ids)) {
                timelineMode[i] = FIRST;
            } else {
                for (TrackEntry next = to.mixingTo; next != null; next = next.mixingTo) {
                    if (next.animation.hasTimeline(ids)) continue;
                    if (next.mixDuration > 0) {
                        timelineMode[i] = HOLD_MIX;
                        timelineHoldMix[i] = next;
                        continue outer;
                    }
                    break;
                }
                timelineMode[i] = HOLD_FIRST;
            }
        }
    }

    public @Null
    TrackEntry getCurrent(int trackIndex) {
        if (trackIndex < 0) throw new IllegalArgumentException("trackIndex must be >= 0.");
        if (trackIndex >= tracks.size) return null;
        return tracks.get(trackIndex);
    }

    // public void addListener(AnimationStateListener listener) {
    //     if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
    //     listeners.add(listener);
    // }

    // public void removeListener(AnimationStateListener listener) {
    //     listeners.removeValue(listener, true);
    // }

    // public void clearListeners() {
    //     listeners.clear();
    // }

    // public void clearListenerNotifications() {
    //     queue.clear();
    // }

    // public float getTimeScale() {
    //     return timeScale;
    // }

    public void setTimeScale(float timeScale) {
        this.timeScale = timeScale;
    }

    // public AnimationStateData getData() {
    //     return data;
    // }

    // public void setData(AnimationStateData data) {
    //     if (data == null) throw new IllegalArgumentException("data cannot be null.");
    //     this.data = data;
    // }

    // public Array<TrackEntry> getTracks() {
    //     return tracks;
    // }

    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        Object[] tracks = this.tracks.items;
        for (int i = 0, n = this.tracks.size; i < n; i++) {
            TrackEntry entry = (TrackEntry) tracks[i];
            if (entry == null) continue;
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(entry.toString());
        }
        if (buffer.length() == 0) return "<none>";
        return buffer.toString();
    }

    private enum EventType {
        start, interrupt, end, dispose, complete, event
    }

    public interface AnimationStateListener {
        void start(TrackEntry entry);

        void interrupt(TrackEntry entry);

        void end(TrackEntry entry);

        void dispose(TrackEntry entry);

        void complete(TrackEntry entry);

        void event(TrackEntry entry, Event event);
    }

    static public class TrackEntry implements Poolable {
        final IntArray timelineMode = new IntArray();
        final Array<TrackEntry> timelineHoldMix = new Array<>();
        final FloatArray timelinesRotation = new FloatArray();
        Animation animation;
        @Null
        TrackEntry previous, next, mixingFrom, mixingTo;
        @Null
        AnimationStateListener listener;
        int trackIndex;
        boolean loop, holdPrevious, reverse;
        float eventThreshold, attachmentThreshold, drawOrderThreshold;
        float animationStart, animationEnd, animationLast, nextAnimationLast;
        float delay, trackTime, trackLast, nextTrackLast, trackEnd, timeScale;
        float alpha, mixTime, mixDuration, interruptAlpha, totalAlpha;
        MixBlend mixBlend = MixBlend.replace;

        public void reset() {
            previous = null;
            next = null;
            mixingFrom = null;
            mixingTo = null;
            animation = null;
            listener = null;
            timelineMode.clear();
            timelineHoldMix.clear();
            timelinesRotation.clear();
        }

        // public int getTrackIndex() {
        //     return trackIndex;
        // }

        // public Animation getAnimation() {
        //     return animation;
        // }

        // public void setAnimation(Animation animation) {
        //     if (animation == null) throw new IllegalArgumentException("animation cannot be null.");
        //     this.animation = animation;
        // }

        // public boolean getLoop() {
        //     return loop;
        // }

        // public void setLoop(boolean loop) {
        //     this.loop = loop;
        // }

        // public float getDelay() {
        //     return delay;
        // }

        // public void setDelay(float delay) {
        //     this.delay = delay;
        // }

        // public float getTrackTime() {
        //     return trackTime;
        // }

        public void setTrackTime(float trackTime) {
            this.trackTime = trackTime;
        }

        // public float getTrackEnd() {
        //     return trackEnd;
        // }

        // public void setTrackEnd(float trackEnd) {
        //     this.trackEnd = trackEnd;
        // }

        public float getTrackComplete() {
            float duration = animationEnd - animationStart;
            if (duration != 0) {
                if (loop) return duration * (1 + (int) (trackTime / duration));
                if (trackTime < duration) return duration;
            }
            return trackTime;
        }

        // public float getAnimationStart() {
        //     return animationStart;
        // }

        // public void setAnimationStart(float animationStart) {
        //     this.animationStart = animationStart;
        // }

        public float getAnimationEnd() {
            return animationEnd;
        }

        // public void setAnimationEnd(float animationEnd) {
        //     this.animationEnd = animationEnd;
        // }

        // public float getAnimationLast() {
        //     return animationLast;
        // }

        // public void setAnimationLast(float animationLast) {
        //     this.animationLast = animationLast;
        //     nextAnimationLast = animationLast;
        // }

        public float getAnimationTime() {
            if (loop) {
                float duration = animationEnd - animationStart;
                if (duration == 0) return animationStart;
                return (trackTime % duration) + animationStart;
            }
            return Math.min(trackTime + animationStart, animationEnd);
        }

        // public float getTimeScale() {
        //     return timeScale;
        // }

        // public void setTimeScale(float timeScale) {
        //     this.timeScale = timeScale;
        // }

        // public @Null AnimationStateListener getListener() {
        //     return listener;
        // }

        // public void setListener(@Null AnimationStateListener listener) {
        //     this.listener = listener;
        // }

        // public float getAlpha() {
        //     return alpha;
        // }

        // public void setAlpha(float alpha) {
        //     this.alpha = alpha;
        // }

        // public float getEventThreshold() {
        //     return eventThreshold;
        // }

        // public void setEventThreshold(float eventThreshold) {
        //     this.eventThreshold = eventThreshold;
        // }

        // public float getAttachmentThreshold() {
        //     return attachmentThreshold;
        // }

        // public void setAttachmentThreshold(float attachmentThreshold) {
        //     this.attachmentThreshold = attachmentThreshold;
        // }

        // public float getDrawOrderThreshold() {
        //     return drawOrderThreshold;
        // }

        // public void setDrawOrderThreshold(float drawOrderThreshold) {
        //     this.drawOrderThreshold = drawOrderThreshold;
        // }

        // public @Null TrackEntry getNext() {
        //     return next;
        // }

        // public @Null TrackEntry getPrevious() {
        //     return previous;
        // }

        // public boolean isComplete() {
        //     return trackTime >= animationEnd - animationStart;
        // }

        // public float getMixTime() {
        //     return mixTime;
        // }

        // public void setMixTime(float mixTime) {
        //     this.mixTime = mixTime;
        // }

        // public float getMixDuration() {
        //     return mixDuration;
        // }

        // public void setMixDuration(float mixDuration) {
        //     this.mixDuration = mixDuration;
        // }

        // public MixBlend getMixBlend() {
        //     return mixBlend;
        // }

        // public void setMixBlend(MixBlend mixBlend) {
        //     if (mixBlend == null) throw new IllegalArgumentException("mixBlend cannot be null.");
        //     this.mixBlend = mixBlend;
        // }

        // public @Null TrackEntry getMixingFrom() {
        //     return mixingFrom;
        // }

        // public @Null TrackEntry getMixingTo() {
        //     return mixingTo;
        // }

        // public boolean getHoldPrevious() {
        //     return holdPrevious;
        // }

        // public void setHoldPrevious(boolean holdPrevious) {
        //     this.holdPrevious = holdPrevious;
        // }

        // public void resetRotationDirections() {
        //     timelinesRotation.clear();
        // }

        // public boolean getReverse() {
        //     return reverse;
        // }

        // public void setReverse(boolean reverse) {
        //     this.reverse = reverse;
        // }

        public String toString() {
            return animation == null ? "<none>" : animation.name;
        }
    }

    // static public abstract class AnimationStateAdapter implements AnimationStateListener {
    //     public void start(TrackEntry entry) {
    //     }
    //
    //     public void interrupt(TrackEntry entry) {
    //     }
    //
    //     public void end(TrackEntry entry) {
    //     }
    //
    //     public void dispose(TrackEntry entry) {
    //     }
    //
    //     public void complete(TrackEntry entry) {
    //     }
    //
    //     public void event(TrackEntry entry, Event event) {
    //     }
    // }

    class EventQueue {
        private final Array objects = new Array<>();
        boolean drainDisabled;

        void start(TrackEntry entry) {
            objects.add(EventType.start);
            objects.add(entry);
            animationsChanged = true;
        }

        void interrupt(TrackEntry entry) {
            objects.add(EventType.interrupt);
            objects.add(entry);
        }

        void end(TrackEntry entry) {
            objects.add(EventType.end);
            objects.add(entry);
            animationsChanged = true;
        }

        void dispose(TrackEntry entry) {
            objects.add(EventType.dispose);
            objects.add(entry);
        }

        void complete(TrackEntry entry) {
            objects.add(EventType.complete);
            objects.add(entry);
        }

        void event(TrackEntry entry, Event event) {
            objects.add(EventType.event);
            objects.add(entry);
            objects.add(event);
        }

        void drain() {
            if (drainDisabled) return;
            drainDisabled = true;
            SnapshotArray<AnimationStateListener> listenersArray = AnimationState.this.listeners;
            for (int i = 0; i < this.objects.size; i += 2) {
                EventType type = (EventType) objects.get(i);
                TrackEntry entry = (TrackEntry) objects.get(i + 1);
                int listenersCount = listenersArray.size;
                Object[] listeners = listenersArray.begin();
                switch (type) {
                    case start:
                        if (entry.listener != null) entry.listener.start(entry);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).start(entry);
                        break;
                    case interrupt:
                        if (entry.listener != null) entry.listener.interrupt(entry);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).interrupt(entry);
                        break;
                    case end:
                        if (entry.listener != null) entry.listener.end(entry);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).end(entry);
                    case dispose:
                        if (entry.listener != null) entry.listener.dispose(entry);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).dispose(entry);
                        trackEntryPool.free(entry);
                        break;
                    case complete:
                        if (entry.listener != null) entry.listener.complete(entry);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).complete(entry);
                        break;
                    case event:
                        Event event = (Event) objects.get(i++ + 2);
                        if (entry.listener != null) entry.listener.event(entry, event);
                        for (int ii = 0; ii < listenersCount; ii++)
                            ((AnimationStateListener) listeners[ii]).event(entry, event);
                        break;
                }
                listenersArray.end();
            }
            clear();
            drainDisabled = false;
        }

        void clear() {
            objects.clear();
        }
    }
}
