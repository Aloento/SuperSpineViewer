package com.esotericsoftware.spine40;

import com.esotericsoftware.spine40.Animation.Timeline;
import com.esotericsoftware.spine40.AnimationState.AnimationStateListener;

/** Stores the current pose values for an {@link Event}.
 * <p>
 * See Timeline
 * {@link Timeline#apply(Skeleton, float, float, com.badlogic.gdx.utils.Array, float, com.esotericsoftware.spine40.Animation.MixBlend, com.esotericsoftware.spine40.Animation.MixDirection)},
 * AnimationStateListener {@link AnimationStateListener#event(com.esotericsoftware.spine40.AnimationState.TrackEntry, Event)}, and
 * <a href="http://esotericsoftware.com/spine-events">Events</a> in the Spine User Guide. */
public class Event {
	private final EventData data;
	int intValue;
	float floatValue;
	String stringValue;
	float volume, balance;
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
		if (stringValue == null) throw new IllegalArgumentException("stringValue cannot be null.");
		this.stringValue = stringValue;
	}

	public float getVolume () {
		return volume;
	}

	public void setVolume (float volume) {
		this.volume = volume;
	}

	public float getBalance () {
		return balance;
	}

	public void setBalance (float balance) {
		this.balance = balance;
	}

	/** The animation time this event was keyed. */
	public float getTime () {
		return time;
	}

	/** The events's setup pose data. */
	public EventData getData () {
		return data;
	}

	public String toString () {
		return data.name;
	}
}
