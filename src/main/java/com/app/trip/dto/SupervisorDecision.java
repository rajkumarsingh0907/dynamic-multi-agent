package com.app.trip.dto;

public record SupervisorDecision(
        NextAction action,
        String reason,
        String instructions
) {
}
