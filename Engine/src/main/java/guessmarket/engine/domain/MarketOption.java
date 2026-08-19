package guessmarket.engine.domain;

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
        purchasedShares = Math.addExact(purchasedShares, quantity);
    }
}
