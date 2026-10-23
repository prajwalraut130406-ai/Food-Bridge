package com.foodbridge.dto;

/** Source (hotel) and destination (NGO) of a delivery, for the volunteer's route map. Coordinates may be null. */
public record RouteView(
        Long donationId,
        String sourceName,
        String sourceAddress,
        Double sourceLat,
        Double sourceLng,
        String destName,
        String destAddress,
        Double destLat,
        Double destLng) {
}
