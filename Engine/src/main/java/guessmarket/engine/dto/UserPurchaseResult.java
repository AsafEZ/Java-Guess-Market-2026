package guessmarket.engine.dto;

import java.util.Objects;

public record UserPurchaseResult(
        String buyerName,
        int eventId,
        int optionNumber,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid,
        MarketEventDetails updatedEvent,
        UserDetails updatedBuyer) {

    public UserPurchaseResult {
        Objects.requireNonNull(buyerName, "buyerName");
        Objects.requireNonNull(updatedEvent, "updatedEvent");
        Objects.requireNonNull(updatedBuyer, "updatedBuyer");
    }
}
