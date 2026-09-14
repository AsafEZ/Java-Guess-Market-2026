package guessmarket.engine.trading.orderbook;

import java.util.List;
import java.util.Objects;

public record OrderSubmissionOutcome(
        LimitOrder submittedOrder,
        List<OrderExecution> executions) {

    public OrderSubmissionOutcome {
        Objects.requireNonNull(submittedOrder, "submittedOrder");
        executions = List.copyOf(Objects.requireNonNull(executions, "executions"));
    }
}
