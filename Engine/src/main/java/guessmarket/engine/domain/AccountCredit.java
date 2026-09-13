package guessmarket.engine.domain;

import java.util.Objects;

public record AccountCredit(String userName, double amount) {
    public AccountCredit {
        String normalized = Objects.requireNonNull(userName, "userName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("Credit amount must be a positive finite number.");
        }
        userName = normalized;
    }
}
