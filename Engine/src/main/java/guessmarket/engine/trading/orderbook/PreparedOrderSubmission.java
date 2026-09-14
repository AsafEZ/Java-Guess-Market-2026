package guessmarket.engine.trading.orderbook;

import java.util.List;
import java.util.Objects;

public record PreparedOrderSubmission(
        long expectedVersion,
        long followingOrderId,
        long followingExecutionId,
        List<LimitOrder> resultingOpenOrders,
        OrderSubmissionOutcome outcome) {

    public PreparedOrderSubmission {
        resultingOpenOrders = List.copyOf(
                Objects.requireNonNull(resultingOpenOrders, "resultingOpenOrders"));
        Objects.requireNonNull(outcome, "outcome");
    }
}
