package guessmarket.engine.domain;

import java.util.List;
import java.util.Objects;

public record SettlementOutcome(
        int eventId,
        int winningOptionNumber,
        String marketMakerName,
        List<UserSettlement> userSettlements,
        List<AccountCredit> accountCredits,
        double totalGrossPayout,
        double totalClosingCommission,
        double totalWinnerNetPayout,
        double marketMakerResidual,
        double totalMarketMakerCredit,
        double eventBalanceBefore,
        double eventBalanceAfter) {

    public SettlementOutcome {
        if (winningOptionNumber < 1) {
            throw new IllegalArgumentException("Winning option number must be positive.");
        }
        marketMakerName = requireName(marketMakerName);
        userSettlements = List.copyOf(
                Objects.requireNonNull(userSettlements, "userSettlements"));
        accountCredits = List.copyOf(
                Objects.requireNonNull(accountCredits, "accountCredits"));
        requireFiniteNonNegative(totalGrossPayout, "totalGrossPayout");
        requireFiniteNonNegative(totalClosingCommission, "totalClosingCommission");
        requireFiniteNonNegative(totalWinnerNetPayout, "totalWinnerNetPayout");
        requireFiniteNonNegative(marketMakerResidual, "marketMakerResidual");
        requireFiniteNonNegative(totalMarketMakerCredit, "totalMarketMakerCredit");
        requireFiniteNonNegative(eventBalanceBefore, "eventBalanceBefore");
        if (Double.compare(eventBalanceAfter, 0.0) != 0) {
            throw new IllegalArgumentException("Settled event balance must be zero.");
        }
    }

    static SettlementOutcome from(SettlementPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new SettlementOutcome(
                plan.eventId(),
                plan.winningOptionNumber(),
                plan.marketMakerName(),
                plan.userSettlements(),
                plan.accountCredits(),
                plan.totalGrossPayout(),
                plan.totalClosingCommission(),
                plan.totalWinnerNetPayout(),
                plan.marketMakerResidual(),
                plan.totalMarketMakerCredit(),
                plan.eventBalanceBefore(),
                0.0);
    }

    private static String requireName(String value) {
        String normalized = Objects.requireNonNull(value, "marketMakerName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Market Maker name cannot be blank.");
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
