package com.app.trip.service;

import com.app.trip.dto.NextAction;
import com.app.trip.dto.SupervisorDecision;
import com.app.trip.dto.TripState;
import com.app.trip.dto.WorkflowStep;
import com.app.trip.utility.TripChecks;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SupervisorAgent {

    private final ChatClient client;

    public SupervisorAgent(
            @Qualifier("supervisorClient") ChatClient client
    ) {
        this.client = client;
    }

    public SupervisorDecision decide(
            TripState state,
            List<WorkflowStep> history,
            Set<NextAction> allowedActions,
            int remainingSteps
    ) {
        return client.prompt()
                .user("""
                    Current trip state:
                    %s

                    Java itinerary-validation issues:
                    %s

                    Execution history:
                    %s

                    allowedActions:
                    %s

                    Remaining decision steps:
                    %s

                    Select exactly one allowed action.
                    Include a brief reason and specific instructions.
                    """.formatted(
                        state,
                        TripChecks.itineraryIssues(state),
                        history,
                        allowedActions,
                        remainingSteps
                ))
                .call()
                .entity(SupervisorDecision.class);
    }
}
