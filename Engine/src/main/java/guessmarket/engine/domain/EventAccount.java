package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

public final class EventAccount {
    private double balance;
    private double totalCommissionCollected;

    public EventAccount(double initialSubsidy) {
        this.balance = initialSubsidy;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    void validateCredit(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("Credit amount must be a positive finite number.");
        }
        if (!Double.isFinite(balance + amount)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The event account balance exceeds the supported numeric range.");
        }
    }

    void credit(double amount) {
        validateCredit(amount);
        balance += amount;
    }

    void validatePurchase(double shareCost, double commission) {
        requireFiniteNonNegative(shareCost, "shareCost");
        requireFiniteNonNegative(commission, "commission");

        double totalCharge = shareCost + commission;
        if (!Double.isFinite(totalCharge)
                || !Double.isFinite(balance + totalCharge)
                || !Double.isFinite(totalCommissionCollected + commission)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The purchase exceeds the event account's numeric range.");
        }
    }

    public void recordPurchase(double shareCost, double commission) {
        validatePurchase(shareCost, commission);
        balance += shareCost + commission;
        totalCommissionCollected += commission;
    }

    void validateShareCostCredit(double shareCost) {
        requireFiniteNonNegative(shareCost, "shareCost");
        if (!Double.isFinite(balance + shareCost)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The share cost exceeds the event account's numeric range.");
        }
    }

    void applyValidatedShareCostCredit(double shareCost) {
        balance += shareCost;
    }

    public void settleClosedEvent(double grossPayout, double commission) {
        double netPayout = grossPayout - commission;
        balance -= netPayout;
        totalCommissionCollected += commission;
    }

    private static void requireFiniteNonNegative(double amount, String parameterName) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a finite non-negative number.");
        }
    }
}
