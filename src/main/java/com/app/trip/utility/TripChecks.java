package com.app.trip.utility;

import com.app.trip.dto.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TripChecks {

    private TripChecks() {
    }

    public static void validateResearch(ResearchResult result) {
        require(result != null, "Research result is missing.");

        require(
                result.accommodationOptions() != null
                        && !result.accommodationOptions().isEmpty(),
                "Accommodation options are missing."
        );

        require(
                result.transportOptions() != null
                        && !result.transportOptions().isEmpty(),
                "Transport options are missing."
        );

        require(
                result.attractions() != null
                        && !result.attractions().isEmpty(),
                "Attractions are missing."
        );

        Set<String> accommodationIds = new HashSet<>();

        for (AccommodationOption option : result.accommodationOptions()) {
            require(option != null, "Null accommodation option.");
            uniqueId(option.id(), accommodationIds);
            nonnegative(option.estimatedCostPerNight());
        }

        Set<String> transportIds = new HashSet<>();

        for (TransportOption option : result.transportOptions()) {
            require(option != null, "Null transport option.");
            uniqueId(option.id(), transportIds);
            nonnegative(option.estimatedCostPerDay());
        }

        Set<String> attractionIds = new HashSet<>();

        for (AttractionOption option : result.attractions()) {
            require(option != null, "Null attraction option.");
            uniqueId(option.id(), attractionIds);
            nonnegative(option.estimatedEntryCost());
        }

        nonnegative(result.estimatedFoodCostPerDay());

        require(
                result.assumptions() != null,
                "Research assumptions are missing."
        );
    }

    public static List<String> itineraryIssues(TripState state) {
        List<String> issues = new ArrayList<>();

        if (state.budget() == null) {
            issues.add("Budget is missing.");
            return issues;
        }

        if (!state.budget().withinBudget()) {
            issues.add("Calculated cost exceeds the user's budget.");
        }

        if (state.itinerary() == null
                || state.itinerary().days() == null) {
            issues.add("Itinerary is missing.");
            return issues;
        }

        int expectedDays = state.request().numberOfDays();
        List<DayPlan> days = state.itinerary().days();

        if (days.size() != expectedDays) {
            issues.add("Itinerary must contain exactly "
                    + expectedDays + " days.");
        }

        Set<Integer> dayNumbers = new HashSet<>();
        Set<String> scheduledAttractions = new HashSet<>();

        for (DayPlan day : days) {
            if (day == null) {
                issues.add("Itinerary contains a null day.");
                continue;
            }

            if (day.day() < 1
                    || day.day() > expectedDays
                    || !dayNumbers.add(day.day())) {
                issues.add("Day numbers must be unique and within 1.."
                        + expectedDays + ".");
            }

            if (blank(day.morning())
                    || blank(day.afternoon())
                    || blank(day.evening())) {
                issues.add("Day " + day.day()
                        + " must describe morning, afternoon and evening.");
            }

            if (day.attractionIds() == null) {
                issues.add("Day " + day.day()
                        + " has no attraction ID list.");
                continue;
            }

            for (String id : day.attractionIds()) {
                if (blank(id) || !scheduledAttractions.add(id)) {
                    issues.add(
                            "Attraction IDs must be nonblank "
                                    + "and scheduled only once."
                    );
                }
            }
        }

        Set<String> selectedAttractions = new HashSet<>(
                state.budget().proposal().attractionIds()
        );

        if (!scheduledAttractions.equals(selectedAttractions)) {
            issues.add(
                    "Scheduled attractions must exactly match "
                            + "the budget proposal."
            );
        }

        return List.copyOf(issues);
    }

    public static boolean complete(TripState state) {
        return state.research() != null
                && state.budget() != null
                && state.budget().withinBudget()
                && itineraryIssues(state).isEmpty()
                && state.review() != null
                && state.review().valid()
                && state.review().issues() != null
                && state.review().issues().isEmpty();
    }

    private static void uniqueId(String id, Set<String> seen) {
        require(
                !blank(id) && seen.add(id),
                "Option IDs must be nonblank and unique within their category."
        );
    }

    private static void nonnegative(BigDecimal value) {
        require(
                value != null && value.signum() >= 0,
                "Estimated costs must be present and nonnegative."
        );
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
