package guessmarket.engine.domain;

import java.util.Objects;
import java.util.Optional;

public record Trade(
        long tradeNumber,
        int optionNumber,
        String optionName,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid,
        Optional<String> buyerName) {

    public Trade(
            long tradeNumber,
            int optionNumber,
            String optionName,
            long shareQuantity,
            double shareCost,
            double commission,
            double totalPaid) {
        this(
                tradeNumber,
                optionNumber,
                optionName,
                shareQuantity,
                shareCost,
                commission,
                totalPaid,
                Optional.empty());
    }

    public Trade(
            long tradeNumber,
            int optionNumber,
            String optionName,
            long shareQuantity,
            double shareCost,
            double commission,
            double totalPaid,
            String buyerName) {
        this(
                tradeNumber,
                optionNumber,
                optionName,
                shareQuantity,
                shareCost,
                commission,
                totalPaid,
                Optional.of(normalizeBuyerName(buyerName)));
    }

    public Trade {
        Objects.requireNonNull(optionName, "optionName");
        buyerName = Objects.requireNonNull(buyerName, "buyerName")
                .map(Trade::normalizeBuyerName);
        if (tradeNumber < 1) {
            throw new IllegalArgumentException("Trade number must be positive.");
        }
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must be positive.");
        }
        if (shareQuantity <= 0) {
            throw new IllegalArgumentException("Share quantity must be positive.");
        }
        requireFiniteNonNegative(shareCost, "shareCost");
        requireFiniteNonNegative(commission, "commission");
        if (!Double.isFinite(totalPaid)
                || Double.compare(totalPaid, shareCost + commission) != 0) {
            throw new IllegalArgumentException(
                    "Total paid must be the finite sum of share cost and commission.");
        }
    }

    private static String normalizeBuyerName(String buyerName) {
        String normalizedName = Objects.requireNonNull(buyerName, "buyerName").trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Buyer name cannot be blank.");
        }
        return normalizedName;
    }

    private static void requireFiniteNonNegative(double amount, String parameterName) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite non-negative number.");
        }
    }
}
