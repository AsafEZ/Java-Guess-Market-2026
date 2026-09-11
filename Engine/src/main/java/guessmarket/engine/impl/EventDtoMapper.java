package guessmarket.engine.impl;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.Trade;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.OptionDetails;
import guessmarket.engine.dto.OptionSummary;
import guessmarket.engine.dto.TradeDetails;

import java.util.ArrayList;
import java.util.List;

final class EventDtoMapper {
    private EventDtoMapper() {
    }

    static EventSummary toSummary(MarketEvent event) {
        List<OptionSummary> options = event.getOptions().stream()
                .map(option -> new OptionSummary(option.getOptionNumber(), option.getName()))
                .toList();
        return new EventSummary(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPolicy().percentage(),
                event.getCommissionPolicy().type(),
                options,
                event.getStatus());
    }

    static EventDetails toDetails(MarketEvent event, LmsrCalculator calculator) {
        List<OptionDetails> optionDetails = new ArrayList<>();
        for (int index = 0; index < event.getOptions().size(); index++) {
            MarketOption option = event.getOptions().get(index);
            optionDetails.add(new OptionDetails(
                    option.getOptionNumber(),
                    option.getName(),
                    option.getPurchasedShares(),
                    event.getLmsrOptionValue(option.getOptionNumber())));
        }

        List<TradeDetails> newestFirst = new ArrayList<>();
        List<Trade> trades = event.getTrades();
        for (int index = trades.size() - 1; index >= 0; index--) {
            Trade trade = trades.get(index);
            newestFirst.add(new TradeDetails(
                    trade.tradeNumber(),
                    trade.optionNumber(),
                    trade.optionName(),
                    trade.shareQuantity(),
                    trade.shareCost(),
                    trade.commission(),
                    trade.totalPaid()));
        }

        Integer winnerNumber = event.getWinningOptionNumber();
        String winnerName = winnerNumber == null
                ? null
                : event.findOption(winnerNumber).getName();
        return new EventDetails(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPolicy().percentage(),
                event.getCommissionPolicy().type(),
                event.getStatus(),
                optionDetails,
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissionCollected(),
                newestFirst,
                winnerNumber,
                winnerName);
    }
}