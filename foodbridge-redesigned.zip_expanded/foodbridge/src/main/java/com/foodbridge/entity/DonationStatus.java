package com.foodbridge.entity;

public enum DonationStatus {
    AVAILABLE("bg-success"),
    CLAIMED("bg-warning text-dark"),
    IN_TRANSIT("bg-primary"),
    DELIVERED("bg-secondary");

    private final String badgeClass;

    DonationStatus(String badgeClass) {
        this.badgeClass = badgeClass;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
