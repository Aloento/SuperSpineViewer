package com.esotericsoftware.spine37;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.spine37.attachments.Attachment;

public class Skin {
    final String name;
    final ObjectMap<Key, Attachment> attachments = new ObjectMap<>();
    final Pool<Key> keyPool = new Pool(64) {
        protected Object newObject() {
            return new Key();
        }
    };
    private final Key lookup = new Key();

    public Skin(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }
    
    public void addAttachment(int slotIndex, String name, Attachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        Key key = keyPool.obtain();
        key.set(slotIndex, name);
        attachments.put(key, attachment);
    }
    
    public void addAttachments(Skin skin) {
        for (Entry<Key, Attachment> entry : skin.attachments.entries())
            addAttachment(entry.key.slotIndex, entry.key.name, entry.value);
    }
    
    public Attachment getAttachment(int slotIndex, String name) {
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        lookup.set(slotIndex, name);
        return attachments.get(lookup);
    }

    public void removeAttachment(int slotIndex, String name) {
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        Key key = keyPool.obtain();
        key.set(slotIndex, name);
        attachments.remove(key);
        keyPool.free(key);
    }

    public void findNamesForSlot(int slotIndex, Array<String> names) {
        if (names == null) throw new IllegalArgumentException("names cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        for (Key key : attachments.keys())
            if (key.slotIndex == slotIndex) names.add(key.name);
    }

    public void findAttachmentsForSlot(int slotIndex, Array<Attachment> attachments) {
        if (attachments == null) throw new IllegalArgumentException("attachments cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        for (Entry<Key, Attachment> entry : this.attachments.entries())
            if (entry.key.slotIndex == slotIndex) attachments.add(entry.value);
    }

    public void getAttachments(Array<Attachment> attachments) {
        if (attachments == null) throw new IllegalArgumentException("attachments cannot be null.");
        for (Attachment attachment : this.attachments.values())
            attachments.add(attachment);
    }

    public void clear() {
        for (Key key : attachments.keys())
            keyPool.free(key);
        attachments.clear(1024);
    }

    public int size() {
        return attachments.size;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    void attachAll(Skeleton skeleton, Skin oldSkin) {
        for (Entry<Key, Attachment> entry : oldSkin.attachments.entries()) {
            int slotIndex = entry.key.slotIndex;
            Slot slot = skeleton.slots.get(slotIndex);
            if (slot.attachment == entry.value) {
                Attachment attachment = getAttachment(slotIndex, entry.key.name);
                if (attachment != null) slot.setAttachment(attachment);
            }
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
            switch (RuntimesLoader.spineVersion.get()) {
                case 38, 37 -> hashCode = name.hashCode() + slotIndex * 37;
                case 36 -> hashCode = 31 * (31 + name.hashCode()) + slotIndex;
            }
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
