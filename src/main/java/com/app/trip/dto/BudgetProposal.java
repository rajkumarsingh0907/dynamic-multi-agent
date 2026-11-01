package com.app.trip.dto;

import java.util.List;

public record BudgetProposal(
        String accommodationId,
        String transportId,
        List<String> attractionIds,
        String explanation
) { }
