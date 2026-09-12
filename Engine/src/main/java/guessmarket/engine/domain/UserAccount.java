package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

public final class UserAccount {
    private double balance;
    private UserStatus status = UserStatus.ACTIVE;

    public UserAccount(double initialBalance) {
        requirePositiveFinite(initialBalance, "initialBalance");
        balance = initialBalance;
    }

    public void credit(double amount) {
        requirePositiveFinite(amount, "amount");
        balance = finiteResult(balance + amount);
    }

    public void debit(double amount) {
        requirePositiveFinite(amount, "amount");

        if (status == UserStatus.BLOCKED) {
            throw new EngineException(
                    ErrorCode.USER_ACCOUNT_BLOCKED,
                    "A blocked user account cannot be debited."
            );
        }

        double updatedBalance = finiteResult(balance - amount);
        balance = updatedBalance;

        if (balance < 0.0) {
            status = UserStatus.BLOCKED;
        }
    }

    public boolean canAfford(double amount) {
        requirePositiveFinite(amount, "amount");
        return status == UserStatus.ACTIVE && balance >= amount;
    }

    public double getBalance() {
        return balance;
    }

    public UserStatus getStatus() {
        return status;
    }

    private static void requirePositiveFinite(double value, String parameterName) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    parameterName + " must be a positive finite number."
            );
        }
    }

    private static double finiteResult(double result) {
        if (!Double.isFinite(result)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The account balance exceeds the supported numeric range."
            );
        }
        return result;
    }
}
