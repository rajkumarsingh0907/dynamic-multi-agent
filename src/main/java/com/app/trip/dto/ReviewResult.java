package com.app.trip.dto;

import java.util.List;

public record ReviewResult(
        boolean valid,
        List<String> issues,
        List<String> recommendations,
        String summary
) {
}
