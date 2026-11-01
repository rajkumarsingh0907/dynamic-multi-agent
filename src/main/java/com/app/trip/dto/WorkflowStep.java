package com.app.trip.dto;

public record WorkflowStep(
        int step,
        NextAction action,
        String reason,
        String instructions
) {
}
