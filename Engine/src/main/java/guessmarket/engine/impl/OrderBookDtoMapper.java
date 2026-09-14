package guessmarket.engine.impl;

import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.User;
import guessmarket.engine.dto.LimitOrderDetails;
import guessmarket.engine.dto.OrderBookEventDetails;
import guessmarket.engine.dto.OrderBookOptionDetails;
import guessmarket.engine.dto.OrderExecutionDetails;
import guessmarket.engine.dto.OrderSubmissionResult;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.trading.orderbook.LimitOrder;
import guessmarket.engine.trading.orderbook.OrderBookTradingOperations;
import guessmarket.engine.trading.orderbook.OrderExecution;
import guessmarket.engine.trading.orderbook.OrderSubmissionOutcome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

final class OrderBookDtoMapper {
    private OrderBookDtoMapper() {
    }

    static OrderBookEventDetails toMechanismDetails(
            MarketEvent event,
            OrderBookTradingOperations orderBook) {
        List<OrderBookOptionDetails> options = event.getOptions().stream()
                .map(option -> toOptionDetails(option, orderBook))
                .toList();
        List<OrderExecutionDetails> executionsNewestFirst = new ArrayList<>();
        List<OrderExecution> executions = orderBook.getExecutions();
        for (int index = executions.size() - 1; index >= 0; index--) {
            executionsNewestFirst.add(toExecutionDetails(executions.get(index)));
        }
        return new OrderBookEventDetails(
                orderBook.isMintAllowed(),
                orderBook.getInitialInvestment(),
                orderBook.getD(),
                options,
                executionsNewestFirst);
    }

    static OrderSubmissionResult toSubmissionResult(
            OrderSubmissionOutcome outcome,
            MarketEvent event,
            User submittingUser,
            MarketSystem system) {
        return new OrderSubmissionResult(
                toOrderDetails(outcome.submittedOrder()),
                outcome.executions().stream()
                        .map(OrderBookDtoMapper::toExecutionDetails)
                        .toList(),
                MarketEventDtoMapper.toDetails(event, system),
                UserDtoMapper.toDetails(submittingUser, system));
    }

    static LimitOrderDetails toOrderDetails(LimitOrder order) {
        return new LimitOrderDetails(
                order.orderId(),
                order.userName(),
                order.optionNumber(),
                order.side(),
                order.originalQuantity(),
                order.remainingQuantity(),
                order.limitPrice(),
                order.status());
    }

    static OrderExecutionDetails toExecutionDetails(OrderExecution execution) {
        return new OrderExecutionDetails(
                execution.executionId(),
                execution.optionNumber(),
                execution.buyerName(),
                execution.sellerName(),
                execution.quantity(),
                execution.unitPrice(),
                execution.shareCost(),
                execution.minted());
    }

    private static OrderBookOptionDetails toOptionDetails(
            MarketOption option,
            OrderBookTradingOperations orderBook) {
        List<LimitOrder> openOrders = orderBook.getOpenOrders(
                option.getOptionNumber());
        Comparator<LimitOrder> buyPriority = Comparator
                .comparingDouble(LimitOrder::limitPrice)
                .reversed()
                .thenComparingLong(LimitOrder::orderId);
        Comparator<LimitOrder> sellPriority = Comparator
                .comparingDouble(LimitOrder::limitPrice)
                .thenComparingLong(LimitOrder::orderId);
        return new OrderBookOptionDetails(
                option.getOptionNumber(),
                option.getName(),
                option.getPurchasedShares(),
                mapOrders(openOrders, OrderSide.BUY, buyPriority),
                mapOrders(openOrders, OrderSide.SELL, sellPriority),
                optional(orderBook.getLastPrice(option.getOptionNumber())),
                optional(orderBook.getBestBid(option.getOptionNumber())),
                optional(orderBook.getBestAsk(option.getOptionNumber())),
                optional(orderBook.getMidPrice(option.getOptionNumber())),
                optional(orderBook.getSpread(option.getOptionNumber())));
    }

    private static List<LimitOrderDetails> mapOrders(
            List<LimitOrder> orders,
            OrderSide side,
            Comparator<LimitOrder> priority) {
        return orders.stream()
                .filter(order -> order.side() == side)
                .sorted(priority)
                .map(OrderBookDtoMapper::toOrderDetails)
                .toList();
    }

    private static Optional<Double> optional(OptionalDouble value) {
        return value.isPresent()
                ? Optional.of(value.getAsDouble())
                : Optional.empty();
    }
}
