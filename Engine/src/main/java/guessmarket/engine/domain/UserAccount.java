package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class UserAccount {
    private double balance;
    private UserStatus status = UserStatus.ACTIVE;
    private final Map<Integer, MarketPosition> positionsByEventId = new LinkedHashMap<>();

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

    public boolean hasPosition(int eventId) {
        requirePositiveEventId(eventId);
        return positionsByEventId.containsKey(eventId);
    }

    public void recordExecutedPurchase(
            int eventId,
            int optionNumber,
            long quantity,
            double paidAmount) {
        requirePositiveEventId(eventId);

        MarketPosition position = positionsByEventId.get(eventId);
        if (position == null) {
            MarketPosition newPosition = new MarketPosition(eventId);
            newPosition.recordPurchase(optionNumber, quantity, paidAmount);
            positionsByEventId.put(eventId, newPosition);
            return;
        }

        position.recordPurchase(optionNumber, quantity, paidAmount);
    }

    public long getSharesForOption(int eventId, int optionNumber) {
        MarketPosition position = getPosition(eventId);
        if (position == null) {
            requirePositiveOptionNumber(optionNumber);
            return 0L;
        }
        return position.getSharesForOption(optionNumber);
    }

    public double getAmountPaidForOption(int eventId, int optionNumber) {
        MarketPosition position = getPosition(eventId);
        if (position == null) {
            requirePositiveOptionNumber(optionNumber);
            return 0.0;
        }
        return position.getAmountPaidForOption(optionNumber);
    }

    public long getTotalShares(int eventId) {
        MarketPosition position = getPosition(eventId);
        return position == null ? 0L : position.getTotalShares();
    }

    public double getTotalAmountPaid(int eventId) {
        MarketPosition position = getPosition(eventId);
        return position == null ? 0.0 : position.getTotalAmountPaid();
    }

    public Set<Integer> getPositionEventIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(positionsByEventId.keySet()));
    }

    public double getBalance() {
        return balance;
    }

    public UserStatus getStatus() {
        return status;
    }

    private MarketPosition getPosition(int eventId) {
        requirePositiveEventId(eventId);
        return positionsByEventId.get(eventId);
    }

    private static void requirePositiveEventId(int eventId) {
        if (eventId < 1) {
            throw new IllegalArgumentException("Event id must be positive.");
        }
    }

    private static void requirePositiveOptionNumber(int optionNumber) {
        if (optionNumber < 1) {
            throw new IllegalArgumentException("Option number must be positive.");
        }
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
