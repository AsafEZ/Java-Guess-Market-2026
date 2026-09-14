package guessmarket.engine.dto;

import java.util.Objects;

public record OptionPositionDetails(
        int optionNumber,
        String optionName,
        long shares,
        double amountPaid,
        double commissionPaid,
        boolean winningOption) {

    public OptionPositionDetails {
        Objects.requireNonNull(optionName, "optionName");
    }
}
