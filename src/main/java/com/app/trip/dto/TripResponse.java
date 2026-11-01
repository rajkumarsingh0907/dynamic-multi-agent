package com.app.trip.dto;

import java.util.List;

public record TripResponse(
        WorkflowStatus status,
        String message,
        TripState state,
        List<WorkflowStep> executionTrace
) {
}
