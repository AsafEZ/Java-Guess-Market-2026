package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

record PurchaseQuote(
        int eventId,
        int optionNumber,
        long quantity,
        double shareCost,
        double commission,
        double totalCharge) {

    static PurchaseQuote create(
            int eventId,
            int optionNumber,
            long quantity,
            double shareCost,
            double commission) {
        requireFiniteNonNegative(shareCost, "shareCost");
        requireFiniteNonNegative(commission, "commission");

        double totalCharge = shareCost + commission;
        if (!Double.isFinite(totalCharge)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The total purchase charge exceeds the supported numeric range.");
        }

        return new PurchaseQuote(
                eventId,
                optionNumber,
                quantity,
                shareCost,
                commission,
                totalCharge);
    }

    PurchaseQuote {
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must be positive.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        requireFiniteNonNegative(shareCost, "shareCost");
        requireFiniteNonNegative(commission, "commission");
        if (!Double.isFinite(totalCharge)
                || Double.compare(totalCharge, shareCost + commission) != 0) {
            throw new IllegalArgumentException(
                    "Total charge must be the finite sum of share cost and commission.");
        }
    }

    private static void requireFiniteNonNegative(double amount, String parameterName) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    parameterName + " must be a finite non-negative value.");
        }
    }
}
