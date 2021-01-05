package com.esotericsoftware.spine36;

/** The interface for all constraints. */
public interface Constraint extends Updatable {
	/** The ordinal for the order a skeleton's constraints will be applied. */
	int getOrder();
}
