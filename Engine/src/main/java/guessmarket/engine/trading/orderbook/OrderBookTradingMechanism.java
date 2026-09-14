package guessmarket.engine.trading.orderbook;

import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

public final class OrderBookTradingMechanism implements OrderBookTradingOperations {
    private final boolean mintAllowed;
    private final int initialInvestment;
    private final int d;
    private List<LimitOrder> openOrders = List.of();
    private final List<OrderExecution> executions = new ArrayList<>();
    private long nextOrderId = 1L;
    private long nextExecutionId = 1L;
    private long version;

    public OrderBookTradingMechanism(
            boolean mintAllowed,
            int initialInvestment,
            int d) {
        if (initialInvestment < 0) {
            throw new IllegalArgumentException(
                    "Order Book initial investment cannot be negative.");
        }
        if (d <= 0) {
            throw new IllegalArgumentException("Order Book d must be positive.");
        }
        if (initialInvestment % d != 0) {
            throw new IllegalArgumentException(
                    "Order Book initial investment must be divisible by d.");
        }
        this.mintAllowed = mintAllowed;
        this.initialInvestment = initialInvestment;
        this.d = d;
    }

    @Override
    public boolean isMintAllowed() {
        return mintAllowed;
    }

    @Override
    public int getInitialInvestment() {
        return initialInvestment;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.ORDER_BOOK;
    }

    @Override
    public synchronized PreparedOrderSubmission prepareOrder(
            String userName,
            int optionNumber,
            OrderSide side,
            long quantity,
            double limitPrice) {
        validateOrder(userName, optionNumber, side, quantity, limitPrice);
        long followingOrderId = increment(nextOrderId, "order id");
        Map<Long, LimitOrder> working = new LinkedHashMap<>();
        for (LimitOrder order : openOrders) {
            working.put(order.orderId(), order);
        }

        LimitOrder incoming = new LimitOrder(
                nextOrderId,
                userName.trim(),
                optionNumber,
                side,
                quantity,
                quantity,
                limitPrice);
        List<OrderExecution> newExecutions = new ArrayList<>();
        MatchResult regularMatch = matchRegular(
                incoming, working, newExecutions, nextExecutionId);
        long followingExecutionId = regularMatch.followingExecutionId();
        long remaining = regularMatch.remainingQuantity();

        if (mintAllowed && side == OrderSide.BUY && remaining > 0L) {
            MintResult mint = matchMint(
                    incoming.withRemainingQuantity(remaining),
                    working,
                    newExecutions,
                    followingExecutionId);
            remaining = mint.remainingQuantity();
            followingExecutionId = mint.followingExecutionId();
        }

        working.values().removeIf(order -> order.remainingQuantity() == 0L);
        LimitOrder submitted = incoming.withRemainingQuantity(remaining);
        if (remaining > 0L) {
            working.put(submitted.orderId(), submitted);
        }
        return new PreparedOrderSubmission(
                version,
                followingOrderId,
                followingExecutionId,
                List.copyOf(working.values()),
                new OrderSubmissionOutcome(submitted, newExecutions));
    }

    @Override
    public synchronized OrderSubmissionOutcome applyPreparedOrder(
            PreparedOrderSubmission prepared) {
        if (prepared.expectedVersion() != version) {
            throw new EngineException(
                    ErrorCode.ORDER_BOOK_STATE_MISMATCH,
                    "The Order Book changed after submission planning.");
        }
        long followingVersion = increment(version, "Order Book version");
        openOrders = prepared.resultingOpenOrders();
        executions.addAll(prepared.outcome().executions());
        nextOrderId = prepared.followingOrderId();
        nextExecutionId = prepared.followingExecutionId();
        version = followingVersion;
        return prepared.outcome();
    }

    @Override
    public synchronized List<LimitOrder> getOpenOrders(int optionNumber) {
        requireOption(optionNumber);
        return openOrders.stream()
                .filter(order -> order.optionNumber() == optionNumber)
                .toList();
    }

    @Override
    public synchronized List<OrderExecution> getExecutions() {
        return List.copyOf(executions);
    }

    @Override
    public synchronized Set<String> getPendingParticipantNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (LimitOrder order : openOrders) {
            names.add(order.userName());
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public synchronized long getPendingSellQuantity(
            String userName,
            int optionNumber) {
        requireOption(optionNumber);
        long total = 0L;
        for (LimitOrder order : openOrders) {
            if (order.side() == OrderSide.SELL
                    && order.optionNumber() == optionNumber
                    && order.userName().equals(userName)) {
                try {
                    total = Math.addExact(total, order.remainingQuantity());
                } catch (ArithmeticException exception) {
                    throw overflow("Pending sell quantity overflow.", exception);
                }
            }
        }
        return total;
    }

    @Override
    public synchronized OptionalDouble getBestBid(int optionNumber) {
        requireOption(optionNumber);
        return openOrders.stream()
                .filter(order -> order.optionNumber() == optionNumber)
                .filter(order -> order.side() == OrderSide.BUY)
                .mapToDouble(LimitOrder::limitPrice)
                .max();
    }

    @Override
    public synchronized OptionalDouble getBestAsk(int optionNumber) {
        requireOption(optionNumber);
        return openOrders.stream()
                .filter(order -> order.optionNumber() == optionNumber)
                .filter(order -> order.side() == OrderSide.SELL)
                .mapToDouble(LimitOrder::limitPrice)
                .min();
    }

    @Override
    public synchronized OptionalDouble getLastPrice(int optionNumber) {
        requireOption(optionNumber);
        for (int index = executions.size() - 1; index >= 0; index--) {
            OrderExecution execution = executions.get(index);
            if (execution.optionNumber() == optionNumber) {
                return OptionalDouble.of(execution.unitPrice());
            }
        }
        return OptionalDouble.empty();
    }

    @Override
    public synchronized OptionalDouble getMidPrice(int optionNumber) {
        OptionalDouble bid = getBestBid(optionNumber);
        OptionalDouble ask = getBestAsk(optionNumber);
        if (bid.isEmpty() || ask.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((bid.getAsDouble() + ask.getAsDouble()) / 2.0);
    }

    @Override
    public synchronized OptionalDouble getSpread(int optionNumber) {
        OptionalDouble bid = getBestBid(optionNumber);
        OptionalDouble ask = getBestAsk(optionNumber);
        if (bid.isEmpty() || ask.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(ask.getAsDouble() - bid.getAsDouble());
    }

    private MatchResult matchRegular(
            LimitOrder incoming,
            Map<Long, LimitOrder> working,
            List<OrderExecution> newExecutions,
            long executionId) {
        List<LimitOrder> candidates = working.values().stream()
                .filter(order -> order.optionNumber() == incoming.optionNumber())
                .filter(order -> order.side() != incoming.side())
                .filter(order -> pricesCross(incoming, order))
                .sorted(priorityFor(incoming.side()))
                .toList();
        long remaining = incoming.remainingQuantity();
        long currentExecutionId = executionId;
        for (LimitOrder candidateSnapshot : candidates) {
            if (remaining == 0L) {
                break;
            }
            LimitOrder candidate = working.get(candidateSnapshot.orderId());
            long matched = Math.min(remaining, candidate.remainingQuantity());
            String buyer = incoming.side() == OrderSide.BUY
                    ? incoming.userName() : candidate.userName();
            String seller = incoming.side() == OrderSide.SELL
                    ? incoming.userName() : candidate.userName();
            newExecutions.add(new OrderExecution(
                    currentExecutionId,
                    incoming.optionNumber(),
                    buyer,
                    Optional.of(seller),
                    matched,
                    candidate.limitPrice(),
                    false));
            currentExecutionId = increment(currentExecutionId, "execution id");
            remaining -= matched;
            working.put(
                    candidate.orderId(),
                    candidate.withRemainingQuantity(
                            candidate.remainingQuantity() - matched));
        }
        return new MatchResult(remaining, currentExecutionId);
    }

    private MintResult matchMint(
            LimitOrder incoming,
            Map<Long, LimitOrder> working,
            List<OrderExecution> newExecutions,
            long executionId) {
        int oppositeOption = incoming.optionNumber() == 1 ? 2 : 1;
        List<LimitOrder> candidates = working.values().stream()
                .filter(order -> order.optionNumber() == oppositeOption)
                .filter(order -> order.side() == OrderSide.BUY)
                .filter(order -> order.limitPrice() + incoming.limitPrice() >= d)
                .sorted(Comparator.comparingDouble(LimitOrder::limitPrice)
                        .reversed()
                        .thenComparingLong(LimitOrder::orderId))
                .toList();
        long remaining = incoming.remainingQuantity();
        long currentExecutionId = executionId;
        for (LimitOrder candidateSnapshot : candidates) {
            if (remaining == 0L) {
                break;
            }
            LimitOrder candidate = working.get(candidateSnapshot.orderId());
            long matched = Math.min(remaining, candidate.remainingQuantity());
            double incomingPrice = d - candidate.limitPrice();
            newExecutions.add(new OrderExecution(
                    currentExecutionId, incoming.optionNumber(),
                    incoming.userName(), Optional.empty(), matched,
                    incomingPrice, true));
            currentExecutionId = increment(currentExecutionId, "execution id");
            newExecutions.add(new OrderExecution(
                    currentExecutionId, oppositeOption,
                    candidate.userName(), Optional.empty(), matched,
                    candidate.limitPrice(), true));
            currentExecutionId = increment(currentExecutionId, "execution id");
            remaining -= matched;
            working.put(
                    candidate.orderId(),
                    candidate.withRemainingQuantity(
                            candidate.remainingQuantity() - matched));
        }
        return new MintResult(remaining, currentExecutionId);
    }

    private static Comparator<LimitOrder> priorityFor(OrderSide incomingSide) {
        Comparator<LimitOrder> price = Comparator.comparingDouble(
                LimitOrder::limitPrice);
        if (incomingSide == OrderSide.SELL) {
            price = price.reversed();
        }
        return price.thenComparingLong(LimitOrder::orderId);
    }

    private static boolean pricesCross(LimitOrder incoming, LimitOrder resting) {
        return incoming.side() == OrderSide.BUY
                ? incoming.limitPrice() >= resting.limitPrice()
                : incoming.limitPrice() <= resting.limitPrice();
    }

    private void validateOrder(
            String userName,
            int optionNumber,
            OrderSide side,
            long quantity,
            double limitPrice) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new EngineException(
                    ErrorCode.INVALID_USER_NAME,
                    "Order user name cannot be null or blank.");
        }
        requireOption(optionNumber);
        if (side == null) {
            throw new EngineException(
                    ErrorCode.INVALID_ORDER_SIDE,
                    "Order side is required.");
        }
        if (quantity <= 0L) {
            throw new EngineException(
                    ErrorCode.INVALID_SHARE_QUANTITY,
                    "Order quantity must be positive.");
        }
        double maximumPrice = d - 0.01;
        if (!Double.isFinite(limitPrice)
                || limitPrice <= 0.0
                || limitPrice > maximumPrice) {
            throw new EngineException(
                    ErrorCode.INVALID_ORDER_PRICE,
                    "Order price must be positive and no greater than "
                            + maximumPrice + ".");
        }
    }

    private static void requireOption(int optionNumber) {
        if (optionNumber < 1 || optionNumber > 2) {
            throw new EngineException(
                    ErrorCode.INVALID_OPTION_NUMBER,
                    "Order Book option number must be 1 or 2.");
        }
    }

    private static long increment(long value, String name) {
        try {
            return Math.incrementExact(value);
        } catch (ArithmeticException exception) {
            throw overflow(name + " exceeds the supported range.", exception);
        }
    }

    private static EngineException overflow(String message, Throwable cause) {
        return new EngineException(ErrorCode.ARITHMETIC_OVERFLOW, message, cause);
    }

    private record MintResult(long remainingQuantity, long followingExecutionId) {
    }

    private record MatchResult(long remainingQuantity, long followingExecutionId) {
    }
}
