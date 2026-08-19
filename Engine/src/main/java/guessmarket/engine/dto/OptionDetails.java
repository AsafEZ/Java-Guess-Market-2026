package guessmarket.engine.dto;

import java.util.Objects;

public record OptionDetails(
        int optionNumber,
        String name,
        long purchasedShares,
        double currentValue) {

    public OptionDetails {
        Objects.requireNonNull(name, "name");
    }}