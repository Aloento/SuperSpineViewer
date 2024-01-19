package com.esotericsoftware.SpineLegacy;

import com.badlogic.gdx.utils.ObjectFloatMap;

public class AnimationStateData {
    final ObjectFloatMap<Key> animationToMixTime = new ObjectFloatMap<>();
    final Key tempKey = new Key();
    private final SkeletonData skeletonData;
    float defaultMix;

    public AnimationStateData(SkeletonData skeletonData) {
        this.skeletonData = skeletonData;
    }

    public SkeletonData getSkeletonData() {
        return skeletonData;
    }

    // public void setMix(String fromName, String toName, float duration) {
    //     Animation from = skeletonData.findAnimation(fromName);
    //     if (from == null) throw new IllegalArgumentException("Animation not found: " + fromName);
    //     Animation to = skeletonData.findAnimation(toName);
    //     if (to == null) throw new IllegalArgumentException("Animation not found: " + toName);
    //     setMix(from, to, duration);
    // }

    // public void setMix(Animation from, Animation to, float duration) {
    //     if (from == null) throw new IllegalArgumentException("from cannot be null.");
    //     if (to == null) throw new IllegalArgumentException("to cannot be null.");
    //     Key key = new Key();
    //     key.a1 = from;
    //     key.a2 = to;
    //     animationToMixTime.put(key, duration);
    // }

    public float getMix(Animation from, Animation to) {
        tempKey.a1 = from;
        tempKey.a2 = to;
        return animationToMixTime.get(tempKey, defaultMix);
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
    }
}
