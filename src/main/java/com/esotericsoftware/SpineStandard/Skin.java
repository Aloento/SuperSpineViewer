package com.esotericsoftware.SpineStandard;

import com.QYun.SuperSpineViewer.Loader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.SpineStandard.attachments.Attachment;

public class Skin {
    final String name;
    final OrderedMap<SkinEntry, SkinEntry> attachments = new OrderedMap<>();
    final ObjectMap<Key, Attachment> O_attachments = new ObjectMap<>();
    final Array<BoneData> bones = new Array<>();
    final Array<ConstraintData> constraints = new Array<>();
    final Pool<Key> keyPool = new Pool<>(64) {
        protected Key newObject() {
            return new Key();
        }
    };
    private final SkinEntry lookup = new SkinEntry();
    private final Key O_lookup = new Key();

    public Skin(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
        if (Loader.spineVersion == 38)
            this.attachments.orderedKeys().ordered = false;
    }

    public void addAttachment(int slotIndex, String name, Attachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        Key key = keyPool.obtain();
        key.set(slotIndex, name);
        if (Loader.spineVersion < 38)
            O_attachments.put(key, attachment);
    }

    public void setAttachment(int slotIndex, String name, Attachment attachment) {
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
        SkinEntry newEntry = new SkinEntry(slotIndex, name, attachment);
        SkinEntry oldEntry = attachments.put(newEntry, newEntry);
        if (oldEntry != null) {
            oldEntry.attachment = attachment;
        }
    }

    public Attachment getAttachment(int slotIndex, String name) {
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        if (Loader.spineVersion > 37) {
            lookup.set(slotIndex, name);
            SkinEntry entry = attachments.get(lookup);
            return entry != null ? entry.attachment : null;
        } else {
            O_lookup.set(slotIndex, name);
            return O_attachments.get(O_lookup);
        }
    }

    // public Array<SkinEntry> getAttachments() {
    //     return attachments.orderedKeys();
    // }

    // public void clear() {
    //     if (Loader.spineVersion > 37) {
    //         bones.clear();
    //         constraints.clear();
    //         attachments.clear(1024);
    //     } else {
    //         for (Key key : O_attachments.keys())
    //             keyPool.free(key);
    //         O_attachments.clear(1024);
    //     }
    // }

    // public int size() {
    //     return O_attachments.size;
    // }

    // public Array<BoneData> getBones() {
    //     return bones;
    // }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    void attachAll(Skeleton skeleton, Skin oldSkin) {
        if (Loader.spineVersion > 37) {
            for (SkinEntry entry : oldSkin.attachments.keys()) {
                int slotIndex = entry.slotIndex;
                Slot slot = skeleton.slots.get(slotIndex);
                if (slot.attachment == entry.attachment) {
                    Attachment attachment = getAttachment(slotIndex, entry.name);
                    if (attachment != null) slot.setAttachment(attachment);
                }
            }
        } else {
            for (Entry<Key, Attachment> entry : oldSkin.O_attachments.entries()) {
                int slotIndex = entry.key.slotIndex;
                Slot slot = skeleton.slots.get(slotIndex);
                if (slot.attachment == entry.value) {
                    Attachment attachment = getAttachment(slotIndex, entry.key.name);
                    if (attachment != null) slot.setAttachment(attachment);
                }
            }
        }
    }

    static public class SkinEntry {
        int slotIndex;
        String name;
        Attachment attachment;
        private int hashCode;

        SkinEntry() {
            set(0, "");
        }

        SkinEntry(int slotIndex, String name, Attachment attachment) {
            set(slotIndex, name);
            this.attachment = attachment;
        }

        void set(int slotIndex, String name) {
            if (name == null) throw new IllegalArgumentException("name cannot be null.");
            this.slotIndex = slotIndex;
            this.name = name;
            this.hashCode = name.hashCode() + slotIndex * 37;
        }

        // public int getSlotIndex() {
        //     return slotIndex;
        // }

        // public String getName() {
        //     return name;
        // }

        // public Attachment getAttachment() {
        //     return attachment;
        // }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object object) {
            if (object == null) return false;
            SkinEntry other = (SkinEntry) object;
            if (slotIndex != other.slotIndex) return false;
            return name.equals(other.name);
        }

        public String toString() {
            return slotIndex + ":" + name;
        }
    }

    static class Key {
        int slotIndex;
        String name;
        int hashCode;

        public void set(int slotIndex, String name) {
            if (name == null) throw new IllegalArgumentException("name cannot be null.");
            this.slotIndex = slotIndex;
            this.name = name;
            if (Loader.spineVersion > 36)
                hashCode = name.hashCode() + slotIndex * 37;
            else hashCode = 31 * (31 + name.hashCode()) + slotIndex;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object object) {
            if (object == null) return false;
            Key other = (Key) object;
            if (slotIndex != other.slotIndex) return false;
            return name.equals(other.name);
        }

        public String toString() {
            return slotIndex + ":" + name;
        }
    }

}
