package com.app.trip.service;

import com.app.trip.dto.*;
import com.app.trip.utility.TripChecks;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Component
public class TripAgents {

    private final ChatClient researchClient;
    private final ChatClient budgetClient;
    private final ChatClient itineraryClient;
    private final ChatClient reviewClient;

    public TripAgents(
            @Qualifier("researchClient")
            ChatClient researchClient,

            @Qualifier("budgetClient")
            ChatClient budgetClient,

            @Qualifier("itineraryClient")
            ChatClient itineraryClient,

            @Qualifier("reviewClient")
            ChatClient reviewClient
    ) {
        this.researchClient = researchClient;
        this.budgetClient = budgetClient;
        this.itineraryClient = itineraryClient;
        this.reviewClient = reviewClient;
    }

    public ResearchResult research(
            TripState state,
            String instructions
    ) {
        ResearchResult result = required(
                researchClient.prompt()
                        .user(input(state, instructions))
                        .call()
                        .entity(ResearchResult.class)
        );

        TripChecks.validateResearch(result);

        return result;
    }

    public BudgetResult budget(
            TripState state,
            String instructions
    ) {
        ResearchResult research = Objects.requireNonNull(
                state.research(),
                "Research is required before budgeting."
        );

        BudgetProposal proposal = required(
                budgetClient.prompt()
                        .user(input(state, instructions))
                        .call()
                        .entity(BudgetProposal.class)
        );

        List<String> attractionIds = proposal.attractionIds();

        if (attractionIds == null
                || attractionIds.isEmpty()
                || attractionIds.stream().anyMatch(
                id -> id == null || id.isBlank()
        )
                || new HashSet<>(attractionIds).size()
                != attractionIds.size()) {
            throw new IllegalArgumentException(
                    "Budget attraction IDs must be nonempty and unique."
            );
        }

        AccommodationOption accommodationOption =
                research.accommodationOptions().stream()
                        .filter(option -> option.id().equals(
                                proposal.accommodationId()
                        ))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown accommodation ID: "
                                        + proposal.accommodationId()
                        ));

        TransportOption transportOption =
                research.transportOptions().stream()
                        .filter(option -> option.id().equals(
                                proposal.transportId()
                        ))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown transport ID: "
                                        + proposal.transportId()
                        ));

        int days = state.request().numberOfDays();
        int nights = days - 1;

        BigDecimal accommodationCost = accommodationOption
                .estimatedCostPerNight()
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal transportCost = transportOption
                .estimatedCostPerDay()
                .multiply(BigDecimal.valueOf(days));

        BigDecimal foodCost = research
                .estimatedFoodCostPerDay()
                .multiply(BigDecimal.valueOf(days));

        BigDecimal activityCost = BigDecimal.ZERO;

        for (String id : attractionIds) {
            AttractionOption attraction =
                    research.attractions().stream()
                            .filter(option -> option.id().equals(id))
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Unknown attraction ID: " + id
                                    )
                            );

            activityCost = activityCost.add(
                    attraction.estimatedEntryCost()
            );
        }

        BigDecimal totalCost = accommodationCost
                .add(transportCost)
                .add(foodCost)
                .add(activityCost);

        boolean withinBudget = totalCost.compareTo(
                state.request().budget()
        ) <= 0;

        return new BudgetResult(
                proposal,
                accommodationCost,
                transportCost,
                foodCost,
                activityCost,
                totalCost,
                withinBudget
        );
    }

    public ItineraryResult itinerary(
            TripState state,
            String instructions
    ) {
        return required(
                itineraryClient.prompt()
                        .user(input(state, instructions))
                        .call()
                        .entity(ItineraryResult.class)
        );
    }

    public ReviewResult review(
            TripState state,
            String instructions
    ) {
        ReviewResult result = required(
                reviewClient.prompt()
                        .user(input(state, instructions))
                        .call()
                        .entity(ReviewResult.class)
        );

        if (result.issues() == null
                || result.recommendations() == null
                || result.summary() == null
                || result.summary().isBlank()) {
            throw new IllegalArgumentException(
                    "Review result is incomplete."
            );
        }

        if (result.valid() && !result.issues().isEmpty()) {
            throw new IllegalArgumentException(
                    "An approved review cannot contain unresolved issues."
            );
        }

        return result;
    }

    private String input(
            TripState state,
            String instructions
    ) {
        return """
                Current trip state:
                %s

                Java itinerary-validation issues:
                %s

                Supervisor instructions:
                %s

                Produce the complete structured output for your role.
                All prices must be identified as unverified estimates.
                """.formatted(
                state,
                TripChecks.itineraryIssues(state),
                instructions
        );
    }

    private <T> T required(T result) {
        return Objects.requireNonNull(
                result,
                "Agent returned no structured result."
        );
    }
}
