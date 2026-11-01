package com.app.trip.service;

import com.app.trip.dto.*;
import com.app.trip.utility.TripChecks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class DynamicTripOrchestrator {

    private static final Logger log =
            LoggerFactory.getLogger(DynamicTripOrchestrator.class);

    private final SupervisorAgent supervisor;
    private final TripAgents agents;
    private final int maxSteps;

    public DynamicTripOrchestrator(
            SupervisorAgent supervisor,
            TripAgents agents,
            @Value("${trip.workflow.max-steps:12}") int maxSteps
    ) {
        if (maxSteps < 1) {
            throw new IllegalArgumentException(
                    "trip.workflow.max-steps must be positive."
            );
        }

        this.supervisor = supervisor;
        this.agents = agents;
        this.maxSteps = maxSteps;
    }

    public TripResponse plan(TripRequest request) {
        validateRequest(request);

        // State is local to this request.
        TripState state = TripState.start(request);
        List<WorkflowStep> history = new ArrayList<>();

        log.info(
                "Workflow started: destination={}, days={}, budget={}",
                request.destination(),
                request.numberOfDays(),
                request.budget()
        );

        for (int step = 1; step <= maxSteps; step++) {
            Set<NextAction> allowed = allowedActions(state);

            SupervisorDecision decision;

            try {
                decision = supervisor.decide(
                        state,
                        List.copyOf(history),
                        Set.copyOf(allowed),
                        maxSteps - step + 1
                );
            } catch (RuntimeException exception) {
                log.error(
                        "Supervisor failed at step {}",
                        step,
                        exception
                );

                return response(
                        WorkflowStatus.SUPERVISOR_ERROR,
                        "The supervisor call or output conversion failed.",
                        state,
                        history
                );
            }

            if (!validDecision(decision, allowed)) {
                log.warn(
                        "Invalid supervisor decision at step {}: {}",
                        step,
                        decision
                );

                return response(
                        WorkflowStatus.INVALID_DECISION,
                        "The supervisor returned an invalid or unavailable action.",
                        state,
                        history
                );
            }

            history.add(new WorkflowStep(
                    step,
                    decision.action(),
                    decision.reason(),
                    decision.instructions()
            ));

            log.info(
                    "Step {}: action={}, reason={}, instructions={}",
                    step,
                    decision.action(),
                    decision.reason(),
                    decision.instructions()
            );

            long started = System.nanoTime();

            try {
                switch (decision.action()) {
                    case RESEARCH -> {
                        ResearchResult research = agents.research(
                                state,
                                decision.instructions()
                        );

                        state = state.withResearch(research);

                        log.info(
                                "Research generated: accommodationOptions={}, "
                                        + "transportOptions={}, attractions={}",
                                research.accommodationOptions().size(),
                                research.transportOptions().size(),
                                research.attractions().size()
                        );
                    }

                    case BUDGET -> {
                        BudgetResult budget = agents.budget(
                                state,
                                decision.instructions()
                        );

                        state = state.withBudget(budget);

                        log.info(
                                "Java-calculated total={}, maximum={}, "
                                        + "withinBudget={}",
                                budget.totalCost(),
                                request.budget(),
                                budget.withinBudget()
                        );
                    }

                    case ITINERARY -> {
                        state = state.withItinerary(
                                agents.itinerary(
                                        state,
                                        decision.instructions()
                                )
                        );

                        log.info(
                                "Itinerary validation issues: {}",
                                TripChecks.itineraryIssues(state)
                        );
                    }

                    case REVIEW -> {
                        ReviewResult review = agents.review(
                                state,
                                decision.instructions()
                        );

                        state = state.withReview(review);

                        log.info(
                                "Review valid={}, issues={}",
                                review.valid(),
                                review.issues()
                        );
                    }

                    case FINISH -> {
                        return response(
                                WorkflowStatus.COMPLETED,
                                "Estimated plan completed and reviewed. "
                                        + "Prices and availability are unverified.",
                                state,
                                history
                        );
                    }

                    case STOP -> {
                        return response(
                                WorkflowStatus.STOPPED,
                                decision.reason(),
                                state,
                                history
                        );
                    }
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Action {} failed at step {}",
                        decision.action(),
                        step,
                        exception
                );

                return response(
                        WorkflowStatus.AGENT_ERROR,
                        "The selected agent failed or returned invalid data.",
                        state,
                        history
                );
            }

            log.info(
                    "Step {} completed in {} ms",
                    step,
                    (System.nanoTime() - started) / 1_000_000
            );
        }

        // The final permitted action may have been a successful review.
        if (TripChecks.complete(state)) {
            return response(
                    WorkflowStatus.COMPLETED,
                    "Estimated plan completed at the step limit. "
                            + "Prices and availability are unverified.",
                    state,
                    history
            );
        }

        return response(
                WorkflowStatus.STEP_LIMIT_REACHED,
                "No approved plan was completed within the step limit.",
                state,
                history
        );
    }

    private Set<NextAction> allowedActions(TripState state) {
        EnumSet<NextAction> allowed = EnumSet.of(
                NextAction.RESEARCH,
                NextAction.STOP
        );

        if (state.research() != null) {
            allowed.add(NextAction.BUDGET);
        }

        if (state.budget() != null
                && state.budget().withinBudget()) {
            allowed.add(NextAction.ITINERARY);
        }

        if (TripChecks.itineraryIssues(state).isEmpty()) {
            allowed.add(NextAction.REVIEW);
        }

        if (TripChecks.complete(state)) {
            allowed.add(NextAction.FINISH);
        }

        return allowed;
    }

    private boolean validDecision(
            SupervisorDecision decision,
            Set<NextAction> allowed
    ) {
        return decision != null
                && decision.action() != null
                && allowed.contains(decision.action())
                && decision.reason() != null
                && !decision.reason().isBlank()
                && decision.instructions() != null;
    }

    private void validateRequest(TripRequest request) {
        if (request == null
                || request.destination() == null
                || request.destination().isBlank()
                || request.destination().length() > 200
                || request.numberOfDays() < 1
                || request.numberOfDays() > 14
                || request.budget() == null
                || request.budget().signum() <= 0
                || (request.preferences() != null
                && request.preferences().length() > 4000)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Provide a destination, 1–14 days, a positive INR budget "
                            + "and preferences of at most 4000 characters."
            );
        }
    }

    private TripResponse response(
            WorkflowStatus status,
            String message,
            TripState state,
            List<WorkflowStep> history
    ) {
        log.info("Workflow ended: status={}", status);

        return new TripResponse(
                status,
                message,
                state,
                List.copyOf(history)
        );
    }
}
