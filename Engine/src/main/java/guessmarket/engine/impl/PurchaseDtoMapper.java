package guessmarket.engine.impl;

import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.PurchaseOutcome;
import guessmarket.engine.domain.User;
import guessmarket.engine.dto.UserPurchaseResult;

import java.util.Objects;

final class PurchaseDtoMapper {
    private PurchaseDtoMapper() {
    }

    static UserPurchaseResult toResult(
            String buyerName,
            int eventId,
            PurchaseOutcome outcome,
            MarketSystem system) {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(system, "system");
        User buyer = system.getUser(buyerName);
        MarketEvent event = system.getEvent(eventId);

        return new UserPurchaseResult(
                buyer.getName(),
                eventId,
                outcome.optionNumber(),
                outcome.shareQuantity(),
                outcome.shareCost(),
                outcome.commission(),
                outcome.totalPaid(),
                MarketEventDtoMapper.toDetails(event, system),
                UserDtoMapper.toDetails(buyer, system));
    }
}
