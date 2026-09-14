package guessmarket.engine.dto;

import java.util.List;
import java.util.Objects;

public record SettlementResult(
        int eventId,
        int winningOptionNumber,
        String winningOptionName,
        String marketMakerName,
        List<UserSettlementResult> userSettlements,
        List<AccountCreditResult> accountCredits,
        double totalGrossPayout,
        double totalClosingCommission,
        double totalWinnerNetPayout,
        double marketMakerResidual,
        double totalMarketMakerCredit,
        double eventBalanceBefore,
        double eventBalanceAfter,
        MarketEventDetails closedEvent) {

    public SettlementResult {
        Objects.requireNonNull(winningOptionName, "winningOptionName");
        Objects.requireNonNull(marketMakerName, "marketMakerName");
        userSettlements = List.copyOf(
                Objects.requireNonNull(userSettlements, "userSettlements"));
        accountCredits = List.copyOf(
                Objects.requireNonNull(accountCredits, "accountCredits"));
        Objects.requireNonNull(closedEvent, "closedEvent");
    }
}
