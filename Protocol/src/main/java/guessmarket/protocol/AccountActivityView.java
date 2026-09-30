package guessmarket.protocol;

public record AccountActivityView(
        long id,
        String occurredAt,
        String action,
        Integer eventId,
        String eventName,
        double amount,
        double commission,
        double balanceAfter) {
}
