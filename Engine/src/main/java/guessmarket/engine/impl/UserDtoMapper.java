package guessmarket.engine.impl;

import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.Trade;
import guessmarket.engine.domain.User;
import guessmarket.engine.dto.OptionPositionDetails;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class UserDtoMapper {
    private UserDtoMapper() {
    }

    static UserSummary toSummary(User user) {
        Objects.requireNonNull(user, "user");
        return new UserSummary(user.getName(), user.getBalance(), user.getStatus());
    }

    static UserDetails toDetails(User user, MarketSystem system) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(system, "system");

        Set<Integer> marketMakerEventIds = system.getAllEvents().stream()
                .filter(MarketEvent::hasMarketMaker)
                .filter(event -> event.isMarketMaker(user.getName()))
                .map(MarketEvent::getId)
                .collect(Collectors.toSet());
        List<PositionDetails> positions = user.getPositionEventIds().stream()
                .map(system::getEvent)
                .map(event -> toPositionDetails(user, event))
                .toList();

        return new UserDetails(
                user.getName(),
                user.getBalance(),
                user.getStatus(),
                marketMakerEventIds,
                positions);
    }

    static PositionDetails toPositionDetails(User user, MarketEvent event) {
        int eventId = event.getId();
        Integer winner = event.getWinningOptionNumber();
        List<OptionPositionDetails> options = event.getOptions().stream()
                .map(option -> new OptionPositionDetails(
                        option.getOptionNumber(),
                        option.getName(),
                        user.getSharesForOption(eventId, option.getOptionNumber()),
                        user.getAmountPaidForOption(eventId, option.getOptionNumber()),
                        user.getCommissionPaidForOption(eventId, option.getOptionNumber()),
                        Objects.equals(winner, option.getOptionNumber())))
                .toList();

        List<TradeDetails> tradesNewestFirst = new ArrayList<>();
        List<Trade> trades = event.getTrades();
        for (int index = trades.size() - 1; index >= 0; index--) {
            Trade trade = trades.get(index);
            if (trade.buyerName().filter(user.getName()::equals).isPresent()) {
                tradesNewestFirst.add(MarketEventDtoMapper.toTradeDetails(trade));
            }
        }

        Optional<Integer> winningOptionNumber = Optional.ofNullable(winner);
        Optional<String> winningOptionName = winningOptionNumber.map(
                optionNumber -> event.findOption(optionNumber).getName());
        return new PositionDetails(
                user.getName(),
                eventId,
                event.getName(),
                event.getStatus(),
                event.getTradingMethod(),
                event.hasMarketMaker() && event.isMarketMaker(user.getName()),
                options,
                user.getTotalShares(eventId),
                user.getTotalAmountPaid(eventId),
                user.getTotalCommissionPaid(eventId),
                tradesNewestFirst,
                winningOptionNumber,
                winningOptionName);
    }
}
