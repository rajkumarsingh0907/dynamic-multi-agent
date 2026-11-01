package com.app.trip.dto;

import java.math.BigDecimal;

public record AttractionOption(
        String id,
        String name,
        String area,
        BigDecimal estimatedEntryCost
) { }
