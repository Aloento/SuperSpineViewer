package com.esotericsoftware.spine40.attachments;

/** The base class for all attachments. */
abstract public class Attachment {
	String name;

	public Attachment (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		this.name = name;
	}

	/** The attachment's name. */
	public String getName () {
		return name;
	}

	public String toString () {
		return name;
	}

	/** Returns a copy of the attachment. */
	abstract public Attachment copy ();
}
