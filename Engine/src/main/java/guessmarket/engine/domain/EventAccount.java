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

    public void recordPurchase(double shareCost, double commission) {
        balance += shareCost + commission;
        totalCommissionCollected += commission;
    }

    public void settleClosedEvent(double grossPayout, double commission) {
        double netPayout = grossPayout - commission;
        balance -= netPayout;
        totalCommissionCollected += commission;
    }
}
