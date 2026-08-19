package guessmarket.engine.domain;

public record CloseOutcome(
        int winningOptionNumber,
        String winningOptionName,
        double grossPayout,
        double commission,
        double netPayout) {
}
