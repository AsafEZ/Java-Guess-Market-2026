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
        this.eventId = eventId;
    }

    public void recordPurchase(int optionNumber, long quantity, double paidAmount) {
        recordPurchase(optionNumber, quantity, paidAmount, 0.0);
    }

    public void recordPurchase(
            int optionNumber,
            long quantity,
            double paidAmount,
            double commissionPaid) {
        validatePurchase(optionNumber, quantity, paidAmount, commissionPaid);
        applyValidatedPurchase(optionNumber, quantity, paidAmount, commissionPaid);
    }

    void validatePurchase(int optionNumber, long quantity, double paidAmount) {
        validatePurchase(optionNumber, quantity, paidAmount, 0.0);
    }

    void validatePurchase(
            int optionNumber,
            long quantity,
            double paidAmount,
            double commissionPaid) {
        requirePositiveOptionNumber(optionNumber);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        requirePositiveFinite(paidAmount, "paidAmount");
        requireFiniteNonNegative(commissionPaid, "commissionPaid");

        OptionHolding current = holdingsByOption.getOrDefault(
                optionNumber,
                new OptionHolding(0L, 0.0, 0.0));
        addShares(current.shares(), quantity);
        finiteAmount(current.amountPaid() + paidAmount, "paid amount");
        finiteAmount(current.commissionPaid() + commissionPaid, "commission paid");
    }

    void applyValidatedPurchase(int optionNumber, long quantity, double paidAmount) {
        applyValidatedPurchase(optionNumber, quantity, paidAmount, 0.0);
    }

    void applyValidatedPurchase(
            int optionNumber,
            long quantity,
            double paidAmount,
            double commissionPaid) {
        OptionHolding current = holdingsByOption.getOrDefault(
                optionNumber,
                new OptionHolding(0L, 0.0, 0.0));
        holdingsByOption.put(
                optionNumber,
                new OptionHolding(
                        current.shares() + quantity,
                        current.amountPaid() + paidAmount,
                        current.commissionPaid() + commissionPaid));
    }

    void validateSale(int optionNumber, long quantity) {
        requirePositiveOptionNumber(optionNumber);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        OptionHolding current = requireHolding(optionNumber);
        if (current.shares() < quantity) {
            throw new EngineException(
                    ErrorCode.INSUFFICIENT_SHARES,
                    "The seller does not own enough shares.");
        }
        remainingPaidAmount(current, quantity);
    }

    void applyValidatedSale(int optionNumber, long quantity) {
        OptionHolding current = requireHolding(optionNumber);
        long remainingShares = current.shares() - quantity;
        double remainingPaid = remainingPaidAmount(current, quantity);
        holdingsByOption.put(
                optionNumber,
                new OptionHolding(
                        remainingShares,
                        remainingPaid,
                        current.commissionPaid()));
    }

    private static double remainingPaidAmount(
            OptionHolding current,
            long soldQuantity) {
        long remainingShares = current.shares() - soldQuantity;
        if (remainingShares == 0L) {
            return 0.0;
        }
        return finiteAmount(
                current.amountPaid()
                        * ((double) remainingShares / current.shares()),
                "remaining paid amount");
    }

    void validateAdditionalCommission(int optionNumber, double commissionPaid) {
        requirePositiveOptionNumber(optionNumber);
        requireFiniteNonNegative(commissionPaid, "commissionPaid");
        OptionHolding current = requireHolding(optionNumber);
        finiteAmount(current.commissionPaid() + commissionPaid, "commission paid");
    }

    void applyValidatedAdditionalCommission(int optionNumber, double commissionPaid) {
        OptionHolding current = requireHolding(optionNumber);
        holdingsByOption.put(
                optionNumber,
                new OptionHolding(
                        current.shares(),
                        current.amountPaid(),
                        current.commissionPaid() + commissionPaid));
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

    public double getCommissionPaidForOption(int optionNumber) {
        requirePositiveOptionNumber(optionNumber);
        OptionHolding holding = holdingsByOption.get(optionNumber);
        return holding == null ? 0.0 : holding.commissionPaid();
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
            total = finiteAmount(total + holding.amountPaid(), "paid amount");
        }
        return total;
    }

    public double getTotalCommissionPaid() {
        double total = 0.0;
        for (OptionHolding holding : holdingsByOption.values()) {
            total = finiteAmount(total + holding.commissionPaid(), "commission paid");
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

    private static void requireFiniteNonNegative(double value, String parameterName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite non-negative number.");
        }
    }

    private OptionHolding requireHolding(int optionNumber) {
        OptionHolding holding = holdingsByOption.get(optionNumber);
        if (holding == null) {
            throw new IllegalStateException(
                    "Cannot record commission without an existing option holding.");
        }
        return holding;
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

    private static double finiteAmount(double amount, String valueName) {
        if (!Double.isFinite(amount)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The position " + valueName + " exceeds the supported range.");
        }
        return amount;
    }

    private record OptionHolding(long shares, double amountPaid, double commissionPaid) {
    }
}
