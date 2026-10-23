package com.foodbridge.entity;

public enum FoodType {
    VEG("Veg", "bg-success"),
    NON_VEG("Non-Veg", "bg-danger");

    private final String label;
    private final String badgeClass;

    FoodType(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
}
