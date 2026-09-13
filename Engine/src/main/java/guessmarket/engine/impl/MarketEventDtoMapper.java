package guessmarket.engine.impl;

import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.Trade;
import guessmarket.engine.dto.LmsrEventDetails;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.OptionDetails;
import guessmarket.engine.dto.OptionSummary;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.dto.TradingMechanismDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class MarketEventDtoMapper {
    private MarketEventDtoMapper() {
    }

    static MarketEventSummary toSummary(MarketEvent event) {
        Objects.requireNonNull(event, "event");
        return new MarketEventSummary(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStatus(),
                event.getTradingMethod(),
                event.getCommissionPolicy().percentage(),
                event.getCommissionPolicy().type(),
                event.getAccount().getBalance(),
                toOptionSummaries(event),
                marketMakerName(event));
    }

    static MarketEventDetails toDetails(MarketEvent event, MarketSystem system) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(system, "system");

        Optional<Integer> winningOptionNumber = Optional.ofNullable(
                event.getWinningOptionNumber());
        Optional<String> winningOptionName = winningOptionNumber.map(
                optionNumber -> event.findOption(optionNumber).getName());
        List<PositionDetails> participants = system.getAllUsers().stream()
                .filter(user -> user.hasPosition(event.getId()))
                .map(user -> UserDtoMapper.toPositionDetails(user, event))
                .toList();

        return new MarketEventDetails(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStatus(),
                event.getTradingMethod(),
                event.getCommissionPolicy().percentage(),
                event.getCommissionPolicy().type(),
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissionCollected(),
                toOptionSummaries(event),
                marketMakerName(event),
                winningOptionNumber,
                winningOptionName,
                participants,
                toMechanismDetails(event));
    }

    private static TradingMechanismDetails toMechanismDetails(MarketEvent event) {
        return switch (event.getTradingMethod()) {
            case LMSR -> toLmsrDetails(event);
        };
    }

    private static LmsrEventDetails toLmsrDetails(MarketEvent event) {
        List<OptionDetails> options = event.getOptions().stream()
                .map(option -> new OptionDetails(
                        option.getOptionNumber(),
                        option.getName(),
                        option.getPurchasedShares(),
                        event.getLmsrOptionValue(option.getOptionNumber())))
                .toList();
        return new LmsrEventDetails(
                event.getB(),
                options,
                toTradeDetailsNewestFirst(event.getTrades()));
    }

    private static List<OptionSummary> toOptionSummaries(MarketEvent event) {
        return event.getOptions().stream()
                .map(MarketEventDtoMapper::toOptionSummary)
                .toList();
    }

    private static OptionSummary toOptionSummary(MarketOption option) {
        return new OptionSummary(option.getOptionNumber(), option.getName());
    }

    static List<TradeDetails> toTradeDetailsNewestFirst(List<Trade> trades) {
        List<TradeDetails> newestFirst = new ArrayList<>();
        for (int index = trades.size() - 1; index >= 0; index--) {
            newestFirst.add(toTradeDetails(trades.get(index)));
        }
        return List.copyOf(newestFirst);
    }

    static TradeDetails toTradeDetails(Trade trade) {
        return new TradeDetails(
                trade.tradeNumber(),
                trade.optionNumber(),
                trade.optionName(),
                trade.shareQuantity(),
                trade.shareCost(),
                trade.commission(),
                trade.totalPaid());
    }

    private static Optional<String> marketMakerName(MarketEvent event) {
        return event.hasMarketMaker()
                ? Optional.of(event.getMarketMakerName())
                : Optional.empty();
    }
}
