package com.app.trip.controller;

import com.app.trip.dto.TripRequest;
import com.app.trip.dto.TripResponse;
import com.app.trip.service.DynamicTripOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final DynamicTripOrchestrator orchestrator;

    public TripController(
            DynamicTripOrchestrator orchestrator
    ) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/plan")
    public TripResponse plan(
            @RequestBody TripRequest request
    ) {
        return orchestrator.plan(request);
    }
}
