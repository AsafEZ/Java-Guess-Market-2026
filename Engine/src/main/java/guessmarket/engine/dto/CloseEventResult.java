package guessmarket.engine.dto;

import java.util.Objects;

public record CloseEventResult(
        int eventId,
        int winningOptionNumber,
        String winningOptionName,
        double grossPayout,
        double commission,
        double netPayout,
        EventDetails closedEvent) {

    public CloseEventResult {
        Objects.requireNonNull(winningOptionName, "winningOptionName");
        Objects.requireNonNull(closedEvent, "closedEvent");
    }
}
