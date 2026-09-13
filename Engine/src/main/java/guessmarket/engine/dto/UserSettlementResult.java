package guessmarket.engine.dto;

import java.util.Objects;

public record UserSettlementResult(
        String userName,
        long winningShares,
        double grossPayout,
        double closingCommission,
        double netPayout) {

    public UserSettlementResult {
        Objects.requireNonNull(userName, "userName");
    }
}
