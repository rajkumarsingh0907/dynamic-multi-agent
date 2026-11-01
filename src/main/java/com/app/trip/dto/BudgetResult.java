package com.app.trip.dto;

import java.math.BigDecimal;

public record BudgetResult(
        BudgetProposal proposal,
        BigDecimal accommodationCost,
        BigDecimal transportCost,
        BigDecimal foodCost,
        BigDecimal activityCost,
        BigDecimal totalCost,
        boolean withinBudget
) { }
