package com.app.trip.dto;

public record TripState(
        TripRequest request,
        ResearchResult research,
        BudgetResult budget,
        ItineraryResult itinerary,
        ReviewResult review

) {
    public static TripState start(TripRequest request) {
        return new TripState(
                request,
                null,
                null,
                null,
                null
        );
    }

    public TripState withResearch(ResearchResult result) {
        return new TripState(
                request,
                result,
                null,
                null,
                null
        );
    }

    public TripState withBudget(BudgetResult result) {
        return new TripState(
                request,
                research,
                result,
                null,
                null
        );
    }

    public TripState withItinerary(ItineraryResult result) {
        return new TripState(
                request,
                research,
                budget,
                result,
                null
        );
    }

    public TripState withReview(ReviewResult result) {
        return new TripState(
                request,
                research,
                budget,
                itinerary,
                result
        );
    }
}
