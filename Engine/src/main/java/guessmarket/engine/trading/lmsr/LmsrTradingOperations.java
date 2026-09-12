package guessmarket.engine.trading.lmsr;

import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.trading.TradingMechanism;

import java.util.List;

public interface LmsrTradingOperations extends TradingMechanism {
    double calculatePurchaseCost(
            List<MarketOption> options,
            int optionNumber,
            long quantity);

    double calculateOptionValue(
            List<MarketOption> options,
            int optionNumber);

    double calculateInitialSubsidy();

    int getB();
}
