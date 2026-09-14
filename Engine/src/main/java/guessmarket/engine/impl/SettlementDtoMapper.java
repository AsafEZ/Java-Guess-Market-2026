package guessmarket.engine.impl;

import guessmarket.engine.domain.AccountCredit;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.SettlementOutcome;
import guessmarket.engine.domain.UserSettlement;
import guessmarket.engine.dto.AccountCreditResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserSettlementResult;

import java.util.Objects;

final class SettlementDtoMapper {
    private SettlementDtoMapper() {
    }

    static SettlementResult toResult(SettlementOutcome outcome, MarketSystem system) {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(system, "system");
        MarketEvent event = system.getEvent(outcome.eventId());

        return new SettlementResult(
                outcome.eventId(),
                outcome.winningOptionNumber(),
                event.findOption(outcome.winningOptionNumber()).getName(),
                outcome.marketMakerName(),
                outcome.userSettlements().stream()
                        .map(SettlementDtoMapper::toUserSettlementResult)
                        .toList(),
                outcome.accountCredits().stream()
                        .map(SettlementDtoMapper::toAccountCreditResult)
                        .toList(),
                outcome.totalGrossPayout(),
                outcome.totalClosingCommission(),
                outcome.totalWinnerNetPayout(),
                outcome.marketMakerResidual(),
                outcome.totalMarketMakerCredit(),
                outcome.eventBalanceBefore(),
                outcome.eventBalanceAfter(),
                MarketEventDtoMapper.toDetails(event, system));
    }

    private static UserSettlementResult toUserSettlementResult(UserSettlement settlement) {
        return new UserSettlementResult(
                settlement.userName(),
                settlement.winningShares(),
                settlement.grossPayout(),
                settlement.closingCommission(),
                settlement.netPayout());
    }

    private static AccountCreditResult toAccountCreditResult(AccountCredit credit) {
        return new AccountCreditResult(credit.userName(), credit.amount());
    }
}
