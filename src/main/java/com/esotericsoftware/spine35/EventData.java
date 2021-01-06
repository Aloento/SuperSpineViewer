package com.esotericsoftware.spine35;

/**
 * Stores the setup pose values for an {@link Event}.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-events">Events</a> in the Spine User Guide.
 */
public class EventData {
    final String name;
    int intValue;
    float floatValue;
    String stringValue;

    public EventData(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    public int getInt() {
        return intValue;
    }

    public void setInt(int intValue) {
        this.intValue = intValue;
    }

    public float getFloat() {
        return floatValue;
    }

    public void setFloat(float floatValue) {
        this.floatValue = floatValue;
    }

    public String getString() {
        return stringValue;
    }

    public void setString(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * The name of the event, which is unique within the skeleton.
     */
    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
