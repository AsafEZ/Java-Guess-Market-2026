package guessmarket.engine.dto;

import java.util.Objects;

public record AccountCreditResult(String userName, double amount) {
    public AccountCreditResult {
        Objects.requireNonNull(userName, "userName");
    }
}
