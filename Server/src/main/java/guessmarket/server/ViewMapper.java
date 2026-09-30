package guessmarket.server;

import guessmarket.engine.dto.EventUploadResult;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.UserSummary;
import guessmarket.protocol.EventView;
import guessmarket.protocol.OptionView;
import guessmarket.protocol.UploadView;
import guessmarket.protocol.UserView;
import guessmarket.protocol.AccountActivityView;
import guessmarket.engine.dto.AccountActivityDetails;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.LmsrEventDetails;
import guessmarket.engine.dto.OrderBookEventDetails;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.dto.LimitOrderDetails;
import guessmarket.engine.dto.OrderExecutionDetails;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.OptionMarketView;
import guessmarket.protocol.OrderView;
import guessmarket.protocol.ExecutionView;
import guessmarket.protocol.TradeView;
import guessmarket.protocol.PositionView;
import guessmarket.protocol.OptionPositionView;
import guessmarket.protocol.UserDetailsView;

import java.util.List;

import java.util.Set;

final class ViewMapper {
    private ViewMapper() {
    }

    static EventView event(MarketEventSummary source) {
        return new EventView(
                source.eventId(), source.name(), source.description(),
                source.status().name(), source.tradingMethod().name(),
                source.commissionPercentage(), source.commissionType().name(),
                source.accountBalance(), source.marketMakerName().orElse(null),
                source.options().stream()
                        .map(option -> new OptionView(option.optionNumber(), option.name()))
                        .toList());
    }

    static UserView user(UserSummary source, Set<String> marketMakerNames) {
        return new UserView(source.name(), source.balance(), source.status().name(),
                marketMakerNames.contains(source.name()));
    }

    static UploadView upload(EventUploadResult source) {
        return new UploadView(source.uploadedEventCount(),
                source.totalEventCount(), source.eventNames());
    }

    static AccountActivityView activity(AccountActivityDetails source) {
        return new AccountActivityView(source.id(), source.occurredAt(), source.action(),
                source.eventId(), source.eventName(), source.amount(),
                source.commission(), source.balanceAfter());
    }

    static EventDetailsView eventDetails(MarketEventDetails source) {
        EventView summary = new EventView(source.eventId(), source.name(),
                source.description(), source.status().name(),
                source.tradingMethod().name(), source.commissionPercentage(),
                source.commissionType().name(), source.accountBalance(),
                source.marketMakerName().orElse(null),
                source.options().stream()
                        .map(option -> new OptionView(option.optionNumber(), option.name()))
                        .toList());
        Integer lmsrB = null;
        Boolean mintAllowed = null;
        Integer initialInvestment = null;
        Integer d = null;
        List<OptionMarketView> options;
        List<TradeView> trades = List.of();
        List<ExecutionView> executions = List.of();
        if (source.mechanismDetails() instanceof LmsrEventDetails lmsr) {
            lmsrB = lmsr.b();
            options = lmsr.options().stream()
                    .map(option -> new OptionMarketView(option.optionNumber(),
                            option.name(), option.purchasedShares(),
                            option.currentValue(), null, null, null, null, null,
                            List.of(), List.of()))
                    .toList();
            trades = lmsr.tradesNewestFirst().stream().map(ViewMapper::trade).toList();
        } else if (source.mechanismDetails() instanceof OrderBookEventDetails book) {
            mintAllowed = book.mintAllowed();
            initialInvestment = book.initialInvestment();
            d = book.d();
            options = book.options().stream()
                    .map(option -> new OptionMarketView(option.optionNumber(),
                            option.optionName(), option.issuedShares(), null,
                            option.lastPrice().orElse(null), option.bestBid().orElse(null),
                            option.bestAsk().orElse(null), option.midPrice().orElse(null),
                            option.spread().orElse(null),
                            option.buyOrders().stream().map(ViewMapper::order).toList(),
                            option.sellOrders().stream().map(ViewMapper::order).toList()))
                    .toList();
            executions = book.executionsNewestFirst().stream()
                    .map(ViewMapper::execution).toList();
        } else {
            throw new IllegalArgumentException("Unsupported trading method.");
        }
        return new EventDetailsView(summary, source.totalCommissionCollected(),
                source.winningOptionNumber().orElse(null),
                source.winningOptionName().orElse(null), lmsrB, mintAllowed,
                initialInvestment, d, options, trades, executions,
                source.participantPositions().stream().map(ViewMapper::position).toList());
    }

    static UserDetailsView userDetails(UserDetails source) {
        return new UserDetailsView(source.name(), source.balance(),
                source.status().name(), source.marketMakerEventIds().stream().sorted().toList(),
                source.positions().stream().map(ViewMapper::position).toList());
    }

    private static PositionView position(PositionDetails source) {
        return new PositionView(source.userName(), source.eventId(), source.eventName(),
                source.eventStatus().name(), source.tradingMethod().name(),
                source.marketMaker(), source.options().stream()
                        .map(option -> new OptionPositionView(option.optionNumber(),
                                option.optionName(), option.shares(), option.amountPaid(),
                                option.commissionPaid(), option.winningOption()))
                        .toList(), source.totalShares(), source.totalAmountPaid(),
                source.totalCommissionPaid(), source.tradesNewestFirst().stream()
                        .map(ViewMapper::trade).toList(),
                source.winningOptionNumber().orElse(null),
                source.winningOptionName().orElse(null));
    }

    private static TradeView trade(TradeDetails source) {
        return new TradeView(source.tradeNumber(), source.optionNumber(),
                source.optionName(), source.shareQuantity(), source.shareCost(),
                source.commission(), source.totalPaid());
    }

    private static OrderView order(LimitOrderDetails source) {
        return new OrderView(source.orderId(), source.userName(),
                source.optionNumber(), source.side().name(),
                source.originalQuantity(), source.remainingQuantity(),
                source.limitPrice(), source.status().name());
    }

    private static ExecutionView execution(OrderExecutionDetails source) {
        return new ExecutionView(source.executionId(), source.optionNumber(),
                source.buyerName(), source.sellerName().orElse(null),
                source.quantity(), source.unitPrice(), source.shareCost(),
                source.minted());
    }
}
