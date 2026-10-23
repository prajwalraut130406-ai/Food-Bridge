package com.foodbridge.entity;

public enum FoodUnit {
    KG("kg"),
    SERVING("serving"),
    LITER("liter");

    private final String label;

    FoodUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
