package com.archive.SpineStandard;

public class EventData {
    final String name;
    int intValue;
    float floatValue;
    String stringValue, audioPath;
    float volume, balance;

    public EventData(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    // public int getInt() {
    //     return intValue;
    // }

    // public void setInt(int intValue) {
    //     this.intValue = intValue;
    // }

    // public float getFloat() {
    //     return floatValue;
    // }

    // public void setFloat(float floatValue) {
    //     this.floatValue = floatValue;
    // }

    // public String getString() {
    //     return stringValue;
    // }

    // public void setString(String stringValue) {
    //     if (stringValue == null) throw new IllegalArgumentException("stringValue cannot be null.");
    //     this.stringValue = stringValue;
    // }

    // public String getName() {
    //     return name;
    // }

    public String toString() {
        return name;
    }
}
