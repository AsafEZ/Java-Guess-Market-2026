package guessmarket.engine.dto;

import java.util.Objects;

public record PurchaseResult(
        int eventId,
        int optionNumber,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid,
        EventDetails updatedEvent) {

    public PurchaseResult {
        Objects.requireNonNull(updatedEvent, "updatedEvent");
    }}
