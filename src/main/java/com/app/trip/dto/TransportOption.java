package com.app.trip.dto;

import java.math.BigDecimal;

public record TransportOption(
        String id,
        String description,
        BigDecimal estimatedCostPerDay
) {
}
