package com.app.trip.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResearchResult(
        List<AccommodationOption> accommodationOptions,
        List<TransportOption> transportOptions,
        List<AttractionOption> attractions,
        BigDecimal estimatedFoodCostPerDay,
        List<String> assumptions
) {
}
