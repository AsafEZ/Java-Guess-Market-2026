package guessmarket.engine.domain;

import java.util.List;
import java.util.Objects;

record SettlementPlan(
        int eventId,
        int winningOptionNumber,
        String marketMakerName,
        double eventBalanceBefore,
        double payoutPerWinningShare,
        List<UserSettlement> userSettlements,
        List<AccountCredit> accountCredits,
        double totalGrossPayout,
        double totalClosingCommission,
        double totalWinnerNetPayout,
        double marketMakerResidual,
        double totalMarketMakerCredit) {

    SettlementPlan {
        if (eventId < 1) {
            throw new IllegalArgumentException("Event id must be positive.");
        }
        if (winningOptionNumber < 1) {
            throw new IllegalArgumentException("Winning option number must be positive.");
        }
        marketMakerName = requireName(marketMakerName, "marketMakerName");
        requireFiniteNonNegative(eventBalanceBefore, "eventBalanceBefore");
        requirePositiveFinite(payoutPerWinningShare, "payoutPerWinningShare");
        userSettlements = List.copyOf(
                Objects.requireNonNull(userSettlements, "userSettlements"));
        accountCredits = List.copyOf(
                Objects.requireNonNull(accountCredits, "accountCredits"));
        requireFiniteNonNegative(totalGrossPayout, "totalGrossPayout");
        requireFiniteNonNegative(totalClosingCommission, "totalClosingCommission");
        requireFiniteNonNegative(totalWinnerNetPayout, "totalWinnerNetPayout");
        requireFiniteNonNegative(marketMakerResidual, "marketMakerResidual");
        requireFiniteNonNegative(totalMarketMakerCredit, "totalMarketMakerCredit");
    }

    private static String requireName(String value, String parameterName) {
        String normalized = Objects.requireNonNull(value, parameterName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be blank.");
        }
        return normalized;
    }

    private static void requirePositiveFinite(double value, String parameterName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a positive finite number.");
        }
    }

    private static void requireFiniteNonNegative(double value, String parameterName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite non-negative number.");
        }
    }
}

record UserSettlement(
        String userName,
        long winningShares,
        double grossPayout,
        double closingCommission,
        double netPayout) {

    UserSettlement {
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

record AccountCredit(String userName, double amount) {
    AccountCredit {
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
