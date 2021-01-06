package com.esotericsoftware.spine35.attachments;

/**
 * The base class for all attachments.
 */
abstract public class Attachment {
    String name;

    public Attachment(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    /**
     * The attachment's name.
     */
    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }
}
