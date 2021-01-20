package com.esotericsoftware.SpinePreview;

public class Event {
    final float time;
    private final EventData data;
    int intValue;
    float floatValue;
    String stringValue;
    float volume, balance;

    public Event(float time, EventData data) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        this.time = time;
        this.data = data;
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
        if (stringValue == null) throw new IllegalArgumentException("stringValue cannot be null.");
        this.stringValue = stringValue;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }


    public float getTime() {
        return time;
    }


    public EventData getData() {
        return data;
    }

    public String toString() {
        return data.name;
    }
}
