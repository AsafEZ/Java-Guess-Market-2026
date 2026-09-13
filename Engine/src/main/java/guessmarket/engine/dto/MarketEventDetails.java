package guessmarket.engine.dto;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MarketEventDetails(
        int eventId,
        String name,
        String description,
        EventStatus status,
        TradingMethod tradingMethod,
        int commissionPercentage,
        CommissionType commissionType,
        double accountBalance,
        double totalCommissionCollected,
        List<OptionSummary> options,
        Optional<String> marketMakerName,
        Optional<Integer> winningOptionNumber,
        Optional<String> winningOptionName,
        List<PositionDetails> participantPositions,
        TradingMechanismDetails mechanismDetails) {

    public MarketEventDetails {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(tradingMethod, "tradingMethod");
        Objects.requireNonNull(commissionType, "commissionType");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        marketMakerName = Objects.requireNonNull(marketMakerName, "marketMakerName");
        winningOptionNumber = Objects.requireNonNull(
                winningOptionNumber, "winningOptionNumber");
        winningOptionName = Objects.requireNonNull(winningOptionName, "winningOptionName");
        participantPositions = List.copyOf(
                Objects.requireNonNull(participantPositions, "participantPositions"));
        Objects.requireNonNull(mechanismDetails, "mechanismDetails");
        if (tradingMethod != mechanismDetails.tradingMethod()) {
            throw new IllegalArgumentException(
                    "Trading method must match the mechanism details.");
        }
        if (winningOptionNumber.isPresent() != winningOptionName.isPresent()) {
            throw new IllegalArgumentException(
                    "Winning option number and name must be present together.");
        }
    }
}
