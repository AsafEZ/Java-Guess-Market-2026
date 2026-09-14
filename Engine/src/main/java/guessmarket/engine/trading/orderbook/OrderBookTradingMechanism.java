package guessmarket.engine.trading.orderbook;

import guessmarket.engine.enums.TradingMethod;

public final class OrderBookTradingMechanism implements OrderBookTradingOperations {
    private final boolean mintAllowed;
    private final int initialInvestment;
    private final int d;

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
}
