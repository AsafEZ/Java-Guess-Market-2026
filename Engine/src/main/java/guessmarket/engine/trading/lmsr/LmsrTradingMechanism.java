package guessmarket.engine.trading.lmsr;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.TradingMechanism;

import java.util.List;
import java.util.Objects;

public final class LmsrTradingMechanism implements TradingMechanism {

    private final int b;
    private final LmsrCalculator calculator;

    public LmsrTradingMechanism(
            int b,
            LmsrCalculator calculator) {

        if (b <= 0) {
            throw new IllegalArgumentException(
                    "LMSR parameter b must be positive.");
        }

        this.b = b;
        this.calculator =
                Objects.requireNonNull(calculator, "calculator");
    }

    public double executePurchase(
            List<MarketOption> options,
            MarketOption selectedOption,
            long quantity) {

        int optionIndex =
                selectedOption.getOptionNumber() - 1;

        long firstShares =
                options.get(0).getPurchasedShares();

        long secondShares =
                options.get(1).getPurchasedShares();

        try {
            double shareCost = calculator.purchaseCost(
                    b,
                    firstShares,
                    secondShares,
                    optionIndex,
                    quantity
            );

            selectedOption.addShares(quantity);

            return shareCost;
        } catch (ArithmeticException exception) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The requested purchase is too large.",
                    exception
            );
        }
    }

    private LmsrTradingMechanism requireLmsrMechanism() {
        if (tradingMechanism instanceof LmsrTradingMechanism lmsrMechanism) {

            return lmsrMechanism;
        }

        throw new EngineException(
                ErrorCode.WRONG_TRADING_METHOD,
                "Event " + id + " does not use LMSR."
        );
    }



    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.LMSR;
    }


    public int getB() {
        return b;
    }
}