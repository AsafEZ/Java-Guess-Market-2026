package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class MarketPosition {
    private final int eventId;
    private final Map<Integer, OptionHolding> holdingsByOption = new LinkedHashMap<>();

    public MarketPosition(int eventId) {
        if (eventId < 1) {
            throw new IllegalArgumentException("Event id must be positive.");
        }
        this.eventId = eventId;
    }

    public void recordPurchase(int optionNumber, long quantity, double paidAmount) {
        requirePositiveOptionNumber(optionNumber);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        requirePositiveFinite(paidAmount, "paidAmount");

        OptionHolding current = holdingsByOption.getOrDefault(
                optionNumber,
                new OptionHolding(0L, 0.0));
        long updatedShares = addShares(current.shares(), quantity);
        double updatedAmountPaid = finiteAmount(current.amountPaid() + paidAmount);

        holdingsByOption.put(
                optionNumber,
                new OptionHolding(updatedShares, updatedAmountPaid));
    }

    public int getEventId() {
        return eventId;
    }

    public long getSharesForOption(int optionNumber) {
        requirePositiveOptionNumber(optionNumber);
        OptionHolding holding = holdingsByOption.get(optionNumber);
        return holding == null ? 0L : holding.shares();
    }

    public double getAmountPaidForOption(int optionNumber) {
        requirePositiveOptionNumber(optionNumber);
        OptionHolding holding = holdingsByOption.get(optionNumber);
        return holding == null ? 0.0 : holding.amountPaid();
    }

    public long getTotalShares() {
        long total = 0L;
        for (OptionHolding holding : holdingsByOption.values()) {
            total = addShares(total, holding.shares());
        }
        return total;
    }

    public double getTotalAmountPaid() {
        double total = 0.0;
        for (OptionHolding holding : holdingsByOption.values()) {
            total = finiteAmount(total + holding.amountPaid());
        }
        return total;
    }

    public Set<Integer> getOptionNumbers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(holdingsByOption.keySet()));
    }

    private static void requirePositiveOptionNumber(int optionNumber) {
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must be positive.");
        }
    }

    private static void requirePositiveFinite(double value, String parameterName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a positive finite number.");
        }
    }

    private static long addShares(long current, long quantity) {
        try {
            return Math.addExact(current, quantity);
        } catch (ArithmeticException exception) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The position share quantity exceeds the supported range.",
                    exception);
        }
    }

    private static double finiteAmount(double amount) {
        if (!Double.isFinite(amount)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The position paid amount exceeds the supported range.");
        }
        return amount;
    }

    private record OptionHolding(long shares, double amountPaid) {
    }
}
