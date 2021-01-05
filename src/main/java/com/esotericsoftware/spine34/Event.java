package com.esotericsoftware.spine34;

public class Event {
	final private EventData data;
	int intValue;
	float floatValue;
	String stringValue;
	final float time;

	public Event (float time, EventData data) {
		if (data == null) throw new IllegalArgumentException("data cannot be null.");
		this.time = time;
		this.data = data;
	}

	public int getInt () {
		return intValue;
	}

	public void setInt (int intValue) {
		this.intValue = intValue;
	}

	public float getFloat () {
		return floatValue;
	}

	public void setFloat (float floatValue) {
		this.floatValue = floatValue;
	}

	public String getString () {
		return stringValue;
	}

	public void setString (String stringValue) {
		this.stringValue = stringValue;
	}

	public float getTime () {
		return time;
	}

	public EventData getData () {
		return data;
	}

	public String toString () {
		return data.name;
	}
}
