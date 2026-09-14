package guessmarket.engine.trading.orderbook;

import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.WinningPayoutOperations;

public interface OrderBookTradingOperations
        extends TradingMechanism, WinningPayoutOperations {
    boolean isMintAllowed();

    int getInitialInvestment();

    int getD();

    @Override
    default double getPayoutPerWinningShare() {
        return getD();
    }
}
