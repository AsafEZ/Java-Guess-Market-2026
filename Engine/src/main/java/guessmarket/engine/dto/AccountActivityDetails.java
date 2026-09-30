package guessmarket.engine.dto;

public record AccountActivityDetails(
        long id,
        String occurredAt,
        String action,
        Integer eventId,
        String eventName,
        double amount,
        double commission,
        double balanceAfter) {
}
