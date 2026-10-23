package com.foodbridge.dto;

import com.foodbridge.entity.VolunteerRank;

/** View model for the volunteer dashboard (call as stats.points() in Thymeleaf). */
public record VolunteerStats(
        int points,
        int totalDeliveries,
        VolunteerRank rank,
        VolunteerRank nextRank,
        int progressPercent,
        int pointsToNext) {
}
