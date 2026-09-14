package guessmarket.engine.trading.orderbook;

import java.util.Objects;
import java.util.Optional;

public record OrderExecution(
        long executionId,
        int optionNumber,
        String buyerName,
        Optional<String> sellerName,
        long quantity,
        double unitPrice,
        boolean minted) {

    public OrderExecution {
        if (executionId < 1 || optionNumber < 1 || quantity < 1) {
            throw new IllegalArgumentException("Invalid execution values.");
        }
        buyerName = Objects.requireNonNull(buyerName, "buyerName");
        sellerName = Objects.requireNonNull(sellerName, "sellerName");
        if (!Double.isFinite(unitPrice) || unitPrice <= 0.0
                || !Double.isFinite(unitPrice * quantity)) {
            throw new IllegalArgumentException("Execution price must be positive and finite.");
        }
        if (minted == sellerName.isPresent()) {
            throw new IllegalArgumentException(
                    "Mint executions have no seller; regular executions require one.");
        }
    }

    public double shareCost() {
        return unitPrice * quantity;
    }
}
