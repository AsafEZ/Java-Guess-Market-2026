package guessmarket.engine.dto;

import java.util.Objects;
import java.util.Optional;

public record OrderExecutionDetails(
        long executionId,
        int optionNumber,
        String buyerName,
        Optional<String> sellerName,
        long quantity,
        double unitPrice,
        double shareCost,
        boolean minted) {

    public OrderExecutionDetails {
        Objects.requireNonNull(buyerName, "buyerName");
        sellerName = Objects.requireNonNull(sellerName, "sellerName");
    }
}
