package guessmarket.engine.trading.orderbook;

import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.OrderStatus;

import java.util.Objects;

public record LimitOrder(
        long orderId,
        String userName,
        int optionNumber,
        OrderSide side,
        long originalQuantity,
        long remainingQuantity,
        double limitPrice) {

    public LimitOrder {
        if (orderId < 1 || optionNumber < 1 || originalQuantity < 1
                || remainingQuantity < 0 || remainingQuantity > originalQuantity) {
            throw new IllegalArgumentException("Invalid limit order values.");
        }
        userName = Objects.requireNonNull(userName, "userName");
        side = Objects.requireNonNull(side, "side");
        if (!Double.isFinite(limitPrice) || limitPrice <= 0.0) {
            throw new IllegalArgumentException("Limit price must be positive and finite.");
        }
    }

    public OrderStatus status() {
        if (remainingQuantity == 0L) {
            return OrderStatus.FILLED;
        }
        if (remainingQuantity < originalQuantity) {
            return OrderStatus.PARTIALLY_FILLED;
        }
        return OrderStatus.OPEN;
    }

    LimitOrder withRemainingQuantity(long remaining) {
        return new LimitOrder(
                orderId,
                userName,
                optionNumber,
                side,
                originalQuantity,
                remaining,
                limitPrice);
    }
}
