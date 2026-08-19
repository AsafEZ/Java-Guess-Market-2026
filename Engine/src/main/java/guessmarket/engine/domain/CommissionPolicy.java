package guessmarket.engine.domain;

import guessmarket.engine.enums.CommissionType;

import java.util.Objects;

public record CommissionPolicy(int percentage, CommissionType type) {
    public CommissionPolicy {
        if (percentage < 0 || percentage > 90) {
            throw new IllegalArgumentException("Commission must be between 0 and 90.");
        }
        Objects.requireNonNull(type, "type");
    }

    public double calculate(double amount) {
        return amount * percentage / 100.0;
    }
}
