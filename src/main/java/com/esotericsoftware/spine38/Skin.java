package com.esotericsoftware.spine38;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import com.esotericsoftware.spine38.attachments.Attachment;
import com.esotericsoftware.spine38.attachments.MeshAttachment;

/** Stores attachments by slot index and attachment name.
 * <p>
 * See SkeletonData {@link SkeletonData#defaultSkin}, Skeleton {@link Skeleton#skin}, and
 * <a href="http://esotericsoftware.com/spine-runtime-skins">Runtime skins</a> in the Spine Runtimes Guide. */
public class Skin {
	final String name;
	final OrderedMap<SkinEntry, SkinEntry> attachments = new OrderedMap();
	final Array<BoneData> bones = new Array();
	final Array<ConstraintData> constraints = new Array();
	private final SkinEntry lookup = new SkinEntry();

	public Skin (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		this.name = name;
		this.attachments.orderedKeys().ordered = false;
	}

	/** Adds an attachment to the skin for the specified slot index and name. */
	public void setAttachment (int slotIndex, String name, Attachment attachment) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
		SkinEntry newEntry = new SkinEntry(slotIndex, name, attachment);
		SkinEntry oldEntry = attachments.put(newEntry, newEntry);
		if (oldEntry != null) {
			oldEntry.attachment = attachment;
		}
	}

	/** Adds all attachments, bones, and constraints from the specified skin to this skin. */
	public void addSkin (Skin skin) {
		if (skin == null) throw new IllegalArgumentException("skin cannot be null.");

		for (BoneData data : skin.bones)
			if (!bones.contains(data, true)) bones.add(data);

		for (ConstraintData data : skin.constraints)
			if (!constraints.contains(data, true)) constraints.add(data);

		for (SkinEntry entry : skin.attachments.keys())
			setAttachment(entry.slotIndex, entry.name, entry.attachment);
	}

	/** Adds all bones and constraints and copies of all attachments from the specified skin to this skin. Mesh attachments are not
	 * copied, instead a new linked mesh is created. The attachment copies can be modified without affecting the originals. */
	public void copySkin (Skin skin) {
		if (skin == null) throw new IllegalArgumentException("skin cannot be null.");

		for (BoneData data : skin.bones)
			if (!bones.contains(data, true)) bones.add(data);

		for (ConstraintData data : skin.constraints)
			if (!constraints.contains(data, true)) constraints.add(data);

		for (SkinEntry entry : skin.attachments.keys()) {
			if (entry.attachment instanceof MeshAttachment)
				setAttachment(entry.slotIndex, entry.name, ((MeshAttachment)entry.attachment).newLinkedMesh());
			else
				setAttachment(entry.slotIndex, entry.name, entry.attachment != null ? entry.attachment.copy() : null);
		}
	}

	/** Returns the attachment for the specified slot index and name, or null. */
	public Attachment getAttachment (int slotIndex, String name) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		lookup.set(slotIndex, name);
		SkinEntry entry = attachments.get(lookup);
		return entry != null ? entry.attachment : null;
	}

	/** Removes the attachment in the skin for the specified slot index and name, if any. */
	public void removeAttachment (int slotIndex, String name) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		lookup.set(slotIndex, name);
		attachments.remove(lookup);
	}

	/** Returns all attachments in this skin. */
	public Array<SkinEntry> getAttachments () {
		return attachments.orderedKeys();
	}

	/** Returns all attachments in this skin for the specified slot index. */
	public void getAttachments (int slotIndex, Array<SkinEntry> attachments) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		if (attachments == null) throw new IllegalArgumentException("attachments cannot be null.");
		for (SkinEntry entry : this.attachments.keys())
			if (entry.slotIndex == slotIndex) attachments.add(entry);
	}

	/** Clears all attachments, bones, and constraints. */
	public void clear () {
		attachments.clear(1024);
		bones.clear();
		constraints.clear();
	}

	public Array<BoneData> getBones () {
		return bones;
	}

	public Array<ConstraintData> getConstraints () {
		return constraints;
	}

	/** The skin's name, which is unique across all skins in the skeleton. */
	public String getName () {
		return name;
	}

	public String toString () {
		return name;
	}

	/** Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached. */
	void attachAll (Skeleton skeleton, Skin oldSkin) {
		for (SkinEntry entry : oldSkin.attachments.keys()) {
			int slotIndex = entry.slotIndex;
			Slot slot = skeleton.slots.get(slotIndex);
			if (slot.attachment == entry.attachment) {
				Attachment attachment = getAttachment(slotIndex, entry.name);
				if (attachment != null) slot.setAttachment(attachment);
			}
		}
	}

	/** Stores an entry in the skin consisting of the slot index, name, and attachment **/
	static public class SkinEntry {
		int slotIndex;
		String name;
		Attachment attachment;
		private int hashCode;

		SkinEntry () {
			set(0, "");
		}

		SkinEntry (int slotIndex, String name, Attachment attachment) {
			set(slotIndex, name);
			this.attachment = attachment;
		}

		void set (int slotIndex, String name) {
			if (name == null) throw new IllegalArgumentException("name cannot be null.");
			this.slotIndex = slotIndex;
			this.name = name;
			this.hashCode = name.hashCode() + slotIndex * 37;
		}

		public int getSlotIndex () {
			return slotIndex;
		}

		/** The name the attachment is associated with, equivalent to the skin placeholder name in the Spine editor. */
		public String getName () {
			return name;
		}

		public Attachment getAttachment () {
			return attachment;
		}

		public int hashCode () {
			return hashCode;
		}

		public boolean equals (Object object) {
			if (object == null) return false;
			SkinEntry other = (SkinEntry)object;
			if (slotIndex != other.slotIndex) return false;
            return name.equals(other.name);
        }

		public String toString () {
			return slotIndex + ":" + name;
		}
	}
}
