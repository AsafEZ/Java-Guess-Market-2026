package guessmarket.engine.domain;

import java.util.Objects;

public record Trade(
        long tradeNumber,
        int optionNumber,
        String optionName,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid) {

    public Trade {
        Objects.requireNonNull(optionName, "optionName");
    }
}