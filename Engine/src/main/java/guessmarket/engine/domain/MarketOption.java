package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.Objects;

public final class MarketOption {
    private final int optionNumber;
    private final String name;
    private long purchasedShares;

    public MarketOption(int optionNumber, String name) {
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must start at 1.");
        }
        this.optionNumber = optionNumber;
        this.name = Objects.requireNonNull(name, "name");
    }

    public int getOptionNumber() {
        return optionNumber;
    }

    public String getName() {
        return name;
    }

    public long getPurchasedShares() {
        return purchasedShares;
    }

    public void addShares(long quantity) {
        validateAddShares(quantity);
        applyValidatedAddShares(quantity);
    }

    void validateAddShares(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Share quantity must be positive.");
        }
        try {
            Math.addExact(purchasedShares, quantity);
        } catch (ArithmeticException exception) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The aggregate share quantity exceeds the supported range.",
                    exception);
        }
    }

    void applyValidatedAddShares(long quantity) {
        purchasedShares += quantity;
    }
}
