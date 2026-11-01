package com.app.trip.dto;

import java.util.List;

public record ItineraryResult(
        List<DayPlan> days,
        String explanation
) {
}
