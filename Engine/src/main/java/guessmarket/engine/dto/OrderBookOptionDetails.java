package guessmarket.engine.dto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record OrderBookOptionDetails(
        int optionNumber,
        String optionName,
        long issuedShares,
        List<LimitOrderDetails> buyOrders,
        List<LimitOrderDetails> sellOrders,
        Optional<Double> lastPrice,
        Optional<Double> bestBid,
        Optional<Double> bestAsk,
        Optional<Double> midPrice,
        Optional<Double> spread) {

    public OrderBookOptionDetails {
        Objects.requireNonNull(optionName, "optionName");
        buyOrders = List.copyOf(Objects.requireNonNull(buyOrders, "buyOrders"));
        sellOrders = List.copyOf(Objects.requireNonNull(sellOrders, "sellOrders"));
        lastPrice = Objects.requireNonNull(lastPrice, "lastPrice");
        bestBid = Objects.requireNonNull(bestBid, "bestBid");
        bestAsk = Objects.requireNonNull(bestAsk, "bestAsk");
        midPrice = Objects.requireNonNull(midPrice, "midPrice");
        spread = Objects.requireNonNull(spread, "spread");
    }
}
