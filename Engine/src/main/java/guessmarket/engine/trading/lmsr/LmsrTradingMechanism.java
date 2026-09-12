package guessmarket.engine.trading.lmsr;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.List;
import java.util.Objects;

public final class LmsrTradingMechanism implements LmsrTradingOperations {

    private final int b;
    private final LmsrCalculator calculator;

    public LmsrTradingMechanism(int b, LmsrCalculator calculator) {

        if (b <= 0) {
            throw new IllegalArgumentException(
                    "LMSR parameter b must be positive.");
        }

        this.b = b;
        this.calculator =
                Objects.requireNonNull(calculator, "calculator");
    }

    @Override
    public double calculatePurchaseCost(
            List<MarketOption> options,
            int optionNumber,
            long quantity) {

        int optionIndex = optionNumber - 1;

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

            if (!Double.isFinite(shareCost) || shareCost < 0.0) {
                throw new ArithmeticException("Purchase cost is not a finite non-negative value.");
            }
            return shareCost;
        } catch (ArithmeticException exception) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The requested purchase is too large.",
                    exception
            );
        }
    }


    @Override
    public double calculateOptionValue(
            List<MarketOption> options,
            int optionNumber) {

        int optionIndex = optionNumber - 1;

        long firstShares =
                options.get(0).getPurchasedShares();

        long secondShares =
                options.get(1).getPurchasedShares();

        return calculator.optionValue(
                b,
                firstShares,
                secondShares,
                optionIndex
        );
    }

    @Override
    public double calculateInitialSubsidy() {
        return calculator.initialSubsidy(b);
    }


    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.LMSR;
    }


    @Override
    public int getB() {
        return b;
    }
}
