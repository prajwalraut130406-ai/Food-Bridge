package com.foodbridge.entity;

public enum VolunteerRank {
    BRONZE("Bronze Ambassador", 0, 500, "bg-warning text-dark"),
    SILVER("Silver Hope Deliverer", 501, 1500, "bg-secondary"),
    GOLD("Gold Hunger Hero", 1501, Integer.MAX_VALUE, "bg-success");

    private final String label;
    private final int minPoints;
    private final int maxPoints;
    private final String badgeClass;

    VolunteerRank(String label, int minPoints, int maxPoints, String badgeClass) {
        this.label = label;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public int getMinPoints() { return minPoints; }
    public int getMaxPoints() { return maxPoints; }
    public String getBadgeClass() { return badgeClass; }

    /** Next tier, or null if already at the top. */
    public VolunteerRank next() {
        VolunteerRank[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    public static VolunteerRank fromPoints(int points) {
        for (VolunteerRank r : values()) {
            if (points >= r.minPoints && points <= r.maxPoints) {
                return r;
            }
        }
        return BRONZE;
    }
}
