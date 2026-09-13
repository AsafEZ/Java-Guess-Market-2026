package guessmarket.engine.trading.lmsr;

import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.WinningPayoutOperations;

import java.util.List;

public interface LmsrTradingOperations extends TradingMechanism, WinningPayoutOperations {
    double calculatePurchaseCost(
            List<MarketOption> options,
            int optionNumber,
            long quantity);

    double calculateOptionValue(
            List<MarketOption> options,
            int optionNumber);

    double calculateInitialSubsidy();

    int getB();

    @Override
    default double getPayoutPerWinningShare() {
        return 1.0;
    }
}
