package com.esotericsoftware.SpineLegacy;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;

public class AnimationState {
    private final Array<TrackEntry> tracks = new Array<>();
    private final Array<Event> events = new Array<>();
    private final Array<AnimationStateListener> listeners = new Array<>();
    private final Pool<TrackEntry> trackEntryPool = new Pool<>() {
        protected TrackEntry newObject() {
            return new TrackEntry();
        }
    };
    private final AnimationStateData data;
    private float timeScale = 1;

    public AnimationState(AnimationStateData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.data = data;
    }

    public void update(float delta) {
        delta *= timeScale;
        for (int i = 0; i < tracks.size; i++) {
            TrackEntry current = tracks.get(i);
            if (current == null) continue;
            TrackEntry next = current.next;
            if (next != null) {
                float nextTime = current.lastTime - next.delay;
                if (nextTime >= 0) {
                    float nextDelta = delta * next.timeScale;
                    next.time = nextTime + nextDelta;
                    current.time += delta * current.timeScale;
                    setCurrent(i, next);
                    next.time -= nextDelta;
                    current = next;
                }
            } else if (!current.loop && current.lastTime >= current.endTime) {
                clearTrack(i);
                continue;
            }
            current.time += delta * current.timeScale;
            if (current.previous != null) {
                float previousDelta = delta * current.previous.timeScale;
                current.previous.time += previousDelta;
                current.mixTime += previousDelta;
            }
        }
    }

    public void apply(Skeleton skeleton) {
        Array<Event> events = this.events;
        int listenerCount = listeners.size;
        for (int i = 0; i < tracks.size; i++) {
            TrackEntry current = tracks.get(i);
            if (current == null) continue;
            events.size = 0;
            float time = current.time;
            float lastTime = current.lastTime;
            float endTime = current.endTime;
            boolean loop = current.loop;
            if (!loop && time > endTime) time = endTime;
            TrackEntry previous = current.previous;
            if (previous == null)
                current.animation.mix(skeleton, lastTime, time, loop, events, current.mix);
            else {
                float previousTime = previous.time;
                if (!previous.loop && previousTime > previous.endTime) previousTime = previous.endTime;
                previous.animation.apply(skeleton, previousTime, previousTime, previous.loop, null);
                float alpha = current.mixTime / current.mixDuration * current.mix;
                if (alpha >= 1) {
                    alpha = 1;
                    trackEntryPool.free(previous);
                    current.previous = null;
                }
                current.animation.mix(skeleton, lastTime, time, loop, events, alpha);
            }
            for (int ii = 0, nn = events.size; ii < nn; ii++) {
                Event event = events.get(ii);
                if (current.listener != null) current.listener.event(i, event);
                for (int iii = 0; iii < listenerCount; iii++)
                    listeners.get(iii).event(i, event);
            }
            if (loop ? (lastTime % endTime > time % endTime) : (lastTime < endTime && time >= endTime)) {
                int count = (int) (time / endTime);
                if (current.listener != null) current.listener.complete(i, count);
                for (int ii = 0, nn = listeners.size; ii < nn; ii++)
                    listeners.get(ii).complete(i, count);
            }
            current.lastTime = current.time;
        }
    }

    // public void clearTracks() {
    //     for (int i = 0, n = tracks.size; i < n; i++)
    //         clearTrack(i);
    //     tracks.clear();
    // }

    public void clearTrack(int trackIndex) {
        if (trackIndex >= tracks.size) return;
        TrackEntry current = tracks.get(trackIndex);
        if (current == null) return;
        if (current.listener != null) current.listener.end(trackIndex);
        for (int i = 0, n = listeners.size; i < n; i++)
            listeners.get(i).end(trackIndex);
        tracks.set(trackIndex, null);
        freeAll(current);
        if (current.previous != null) trackEntryPool.free(current.previous);
    }

    private void freeAll(TrackEntry entry) {
        while (entry != null) {
            TrackEntry next = entry.next;
            trackEntryPool.free(entry);
            entry = next;
        }
    }

    private TrackEntry expandToIndex(int index) {
        if (index < tracks.size) return tracks.get(index);
        tracks.ensureCapacity(index - tracks.size + 1);
        tracks.size = index + 1;
        return null;
    }

    private void setCurrent(int index, TrackEntry entry) {
        TrackEntry current = expandToIndex(index);
        if (current != null) {
            TrackEntry previous = current.previous;
            current.previous = null;
            if (current.listener != null) current.listener.end(index);
            for (int i = 0, n = listeners.size; i < n; i++)
                listeners.get(i).end(index);
            entry.mixDuration = data.getMix(current.animation, entry.animation);
            if (entry.mixDuration > 0) {
                entry.mixTime = 0;
                if (previous != null && current.mixTime / current.mixDuration < 0.5f) {
                    entry.previous = previous;
                    previous = current;
                } else
                    entry.previous = current;
            } else
                trackEntryPool.free(current);
            if (previous != null) trackEntryPool.free(previous);
        }
        tracks.set(index, entry);
        if (entry.listener != null) entry.listener.start(index);
        for (int i = 0, n = listeners.size; i < n; i++)
            listeners.get(i).start(index);
    }

    public TrackEntry setAnimation(int trackIndex, String animationName, boolean loop) {
        Animation animation = data.getSkeletonData().findAnimation(animationName);
        if (animation == null) throw new IllegalArgumentException("Animation not found: " + animationName);
        return setAnimation(trackIndex, animation, loop);
    }

    public TrackEntry setAnimation(int trackIndex, Animation animation, boolean loop) {
        TrackEntry current = expandToIndex(trackIndex);
        if (current != null) freeAll(current.next);
        TrackEntry entry = trackEntryPool.obtain();
        entry.animation = animation;
        entry.loop = loop;
        entry.endTime = animation.getDuration();
        setCurrent(trackIndex, entry);
        return entry;
    }

    public TrackEntry getCurrent(int trackIndex) {
        if (trackIndex >= tracks.size) return null;
        return tracks.get(trackIndex);
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = timeScale;
    }

    // public AnimationStateData getData() {
    //     return data;
    // }

    // public void setData(AnimationStateData data) {
    //     this.data = data;
    // }

    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        for (int i = 0, n = tracks.size; i < n; i++) {
            TrackEntry entry = tracks.get(i);
            if (entry == null) continue;
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(entry.toString());
        }
        if (buffer.length() == 0) return "<none>";
        return buffer.toString();
    }

    public interface AnimationStateListener {
        void event(int trackIndex, Event event);

        void complete(int trackIndex, int loopCount);

        void start(int trackIndex);

        void end(int trackIndex);
    }

    static public class TrackEntry implements Poolable {
        TrackEntry next, previous;
        Animation animation;
        boolean loop;
        float delay, time, lastTime = -1, endTime, timeScale = 1;
        float mixTime, mixDuration;
        AnimationStateListener listener;
        float mix = 1;

        public void reset() {
            next = null;
            previous = null;
            animation = null;
            listener = null;
            timeScale = 1;
            lastTime = -1;
            time = 0;
        }

        // public Animation getAnimation() {
        //     return animation;
        // }

        // public void setAnimation(Animation animation) {
        //     this.animation = animation;
        // }

        public boolean getLoop() {
            return loop;
        }

        public float getTime() {
            return time;
        }

        // public void setTime(float time) {
        //     this.time = time;
        // }

        public float getEndTime() {
            return endTime;
        }

        // public float getMix() {
        //     return mix;
        // }

        // public void setMix(float mix) {
        //     this.mix = mix;
        // }

        public String toString() {
            return animation == null ? "<none>" : animation.name;
        }
    }

}
