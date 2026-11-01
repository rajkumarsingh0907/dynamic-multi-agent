package com.app.trip.dto;

import java.util.List;

public record DayPlan(
        int day,
        List<String> attractionIds,
        String morning,
        String afternoon,
        String evening
) {
}
