package com.esotericsoftware.spine37;

/** The interface for all constraints. */
public interface Constraint extends Updatable {
	/** The ordinal for the order a skeleton's constraints will be applied. */
	int getOrder();
}
