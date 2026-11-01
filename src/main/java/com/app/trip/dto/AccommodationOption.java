package com.app.trip.dto;

import java.math.BigDecimal;

public record AccommodationOption(
        String id,
        String description,
        BigDecimal estimatedCostPerNight
) {
}
