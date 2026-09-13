package guessmarket.engine.dto;

import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PositionDetails(
        String userName,
        int eventId,
        String eventName,
        EventStatus eventStatus,
        TradingMethod tradingMethod,
        boolean marketMaker,
        List<OptionPositionDetails> options,
        long totalShares,
        double totalAmountPaid,
        double totalCommissionPaid,
        List<TradeDetails> tradesNewestFirst,
        Optional<Integer> winningOptionNumber,
        Optional<String> winningOptionName) {

    public PositionDetails {
        Objects.requireNonNull(userName, "userName");
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(eventStatus, "eventStatus");
        Objects.requireNonNull(tradingMethod, "tradingMethod");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        tradesNewestFirst = List.copyOf(
                Objects.requireNonNull(tradesNewestFirst, "tradesNewestFirst"));
        winningOptionNumber = Objects.requireNonNull(
                winningOptionNumber, "winningOptionNumber");
        winningOptionName = Objects.requireNonNull(winningOptionName, "winningOptionName");
        if (winningOptionNumber.isPresent() != winningOptionName.isPresent()) {
            throw new IllegalArgumentException(
                    "Winning option number and name must be present together.");
        }
    }
}
