package com.esotericsoftware.SpineStandard.attachments;

abstract public class Attachment {
    String name;

    public Attachment(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    // public String getName() {
    //     return name;
    // }

    public String toString() {
        return name;
    }

    // abstract public Attachment copy();
}
