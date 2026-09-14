package guessmarket.engine.dto;

import java.util.List;
import java.util.Objects;

public record OrderSubmissionResult(
        LimitOrderDetails submittedOrder,
        List<OrderExecutionDetails> executions,
        MarketEventDetails updatedEvent,
        UserDetails updatedSubmittingUser) {

    public OrderSubmissionResult {
        Objects.requireNonNull(submittedOrder, "submittedOrder");
        executions = List.copyOf(Objects.requireNonNull(executions, "executions"));
        Objects.requireNonNull(updatedEvent, "updatedEvent");
        Objects.requireNonNull(updatedSubmittingUser, "updatedSubmittingUser");
    }
}
