package guessmarket.engine.dto;

import guessmarket.engine.enums.UserStatus;

import java.util.Objects;

public record UserSummary(String name, double balance, UserStatus status) {
    public UserSummary {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
    }
}
