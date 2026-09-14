package guessmarket.engine.dto;

import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.OrderStatus;

import java.util.Objects;

public record LimitOrderDetails(
        long orderId,
        String userName,
        int optionNumber,
        OrderSide side,
        long originalQuantity,
        long remainingQuantity,
        double limitPrice,
        OrderStatus status) {

    public LimitOrderDetails {
        Objects.requireNonNull(userName, "userName");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(status, "status");
    }
}
