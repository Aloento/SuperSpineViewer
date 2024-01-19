package com.esotericsoftware.Spine40;

import com.badlogic.gdx.utils.ObjectFloatMap;
import com.esotericsoftware.Spine40.AnimationState.TrackEntry;

/**
 * Stores mix (crossfade) durations to be applied when {@link AnimationState} animations are changed.
 */
public class AnimationStateData {
    final SkeletonData skeletonData;
    final ObjectFloatMap<Key> animationToMixTime = new ObjectFloatMap(51, 0.8f);
    final Key tempKey = new Key();
    float defaultMix;

    public AnimationStateData(SkeletonData skeletonData) {
        if (skeletonData == null) throw new IllegalArgumentException("skeletonData cannot be null.");
        this.skeletonData = skeletonData;
    }

    /**
     * The SkeletonData to look up animations when they are specified by name.
     */
    public SkeletonData getSkeletonData() {
        return skeletonData;
    }

    /**
     * Sets a mix duration by animation name.
     * <p>
     * See {@link #setMix(Animation, Animation, float)}.
     */
    public void setMix(String fromName, String toName, float duration) {
        Animation from = skeletonData.findAnimation(fromName);
        if (from == null) throw new IllegalArgumentException("Animation not found: " + fromName);
        Animation to = skeletonData.findAnimation(toName);
        if (to == null) throw new IllegalArgumentException("Animation not found: " + toName);
        setMix(from, to, duration);
    }

    /**
     * Sets the mix duration when changing from the specified animation to the other.
     * <p>
     * See {@link TrackEntry#mixDuration}.
     */
    public void setMix(Animation from, Animation to, float duration) {
        if (from == null) throw new IllegalArgumentException("from cannot be null.");
        if (to == null) throw new IllegalArgumentException("to cannot be null.");
        Key key = new Key();
        key.a1 = from;
        key.a2 = to;
        animationToMixTime.put(key, duration);
    }

    /**
     * Returns the mix duration to use when changing from the specified animation to the other, or the {@link #getDefaultMix()} if
     * no mix duration has been set.
     */
    public float getMix(Animation from, Animation to) {
        if (from == null) throw new IllegalArgumentException("from cannot be null.");
        if (to == null) throw new IllegalArgumentException("to cannot be null.");
        tempKey.a1 = from;
        tempKey.a2 = to;
        return animationToMixTime.get(tempKey, defaultMix);
    }

    /**
     * The mix duration to use when no mix duration has been defined between two animations.
     */
    public float getDefaultMix() {
        return defaultMix;
    }

    public void setDefaultMix(float defaultMix) {
        this.defaultMix = defaultMix;
    }

    static class Key {
        Animation a1, a2;

        public int hashCode() {
            return 31 * (31 + a1.hashCode()) + a2.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            Key other = (Key) obj;
            if (a1 == null) {
                if (other.a1 != null) return false;
            } else if (!a1.equals(other.a1)) return false;
            if (a2 == null) {
                return other.a2 == null;
            } else return a2.equals(other.a2);
        }

        public String toString() {
            return a1.name + "->" + a2.name;
        }
    }
}
