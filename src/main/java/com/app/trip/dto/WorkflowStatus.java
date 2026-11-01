package com.app.trip.dto;

public enum WorkflowStatus {
    COMPLETED,
    STOPPED,
    STEP_LIMIT_REACHED,
    INVALID_DECISION,
    SUPERVISOR_ERROR,
    AGENT_ERROR
}
