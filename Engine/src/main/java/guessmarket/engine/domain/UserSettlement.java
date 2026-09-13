package guessmarket.engine.domain;

import java.util.Objects;

public record UserSettlement(
        String userName,
        long winningShares,
        double grossPayout,
        double closingCommission,
        double netPayout) {

    public UserSettlement {
        userName = requireName(userName);
        if (winningShares <= 0) {
            throw new IllegalArgumentException("Winning shares must be positive.");
        }
        requireFiniteNonNegative(grossPayout, "grossPayout");
        requireFiniteNonNegative(closingCommission, "closingCommission");
        requireFiniteNonNegative(netPayout, "netPayout");
    }

    private static String requireName(String value) {
        String normalized = Objects.requireNonNull(value, "userName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }
        return normalized;
    }

    private static void requireFiniteNonNegative(double value, String parameterName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite non-negative number.");
        }
    }
}
