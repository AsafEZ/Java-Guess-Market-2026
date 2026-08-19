package guessmarket.engine.dto;

import java.util.Objects;

public record OptionSummary(int optionNumber, String name) {
    public OptionSummary {
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must start at 1.");
        }
        Objects.requireNonNull(name, "name");
    }}