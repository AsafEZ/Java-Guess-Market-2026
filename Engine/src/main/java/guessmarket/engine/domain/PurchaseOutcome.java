package guessmarket.engine.domain;

public record PurchaseOutcome(
        int optionNumber,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid) {
}
