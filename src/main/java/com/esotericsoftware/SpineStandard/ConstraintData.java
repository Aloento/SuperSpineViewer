package com.esotericsoftware.SpineStandard;


abstract public class ConstraintData {
    final String name;
    int order;
    boolean skinRequired;

    public ConstraintData(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    // public String getName() {
    //     return name;
    // }

    // public int getOrder() {
    //     return order;
    // }

    // public void setOrder(int order) {
    //     this.order = order;
    // }

    public String toString() {
        return name;
    }
}
