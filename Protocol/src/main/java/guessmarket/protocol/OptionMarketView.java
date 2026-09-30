package guessmarket.protocol;

import java.util.List;

public record OptionMarketView(int optionNumber, String name, long issuedShares,
                               Double currentValue, Double lastPrice, Double bestBid,
                               Double bestAsk, Double midPrice, Double spread,
                               List<OrderView> buyOrders, List<OrderView> sellOrders) {
}
