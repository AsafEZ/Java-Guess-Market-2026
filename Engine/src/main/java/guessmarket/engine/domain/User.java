package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;

import java.util.Objects;
import java.util.Set;

public final class User {
    private final String name;
    private final UserAccount account;

    public User(String name, double initialBalance) {
        String trimmedName = Objects.requireNonNull(name, "name").trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }

        this.name = trimmedName;
        this.account = new UserAccount(initialBalance);
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return account.getBalance();
    }

    public UserStatus getStatus() {
        return account.getStatus();
    }

    public void credit(double amount) {
        account.credit(amount);
    }

    public void debit(double amount) {
        account.debit(amount);
    }

    public boolean canAfford(double amount) {
        return account.canAfford(amount);
    }

    void validateCredit(double amount) {
        account.validateCredit(amount);
    }

    void applyValidatedCredit(double amount) {
        account.applyValidatedCredit(amount);
    }

    void validateDebit(double amount) {
        account.validateDebit(amount);
    }

    void applyValidatedDebit(double amount) {
        account.applyValidatedDebit(amount);
    }

    public boolean hasPosition(int eventId) {
        return account.hasPosition(eventId);
    }

    public void recordExecutedPurchase(
            int eventId,
            int optionNumber,
            long quantity,
            double paidAmount) {
        account.recordExecutedPurchase(eventId, optionNumber, quantity, paidAmount);
    }

    void validateExecutedPurchase(
            int eventId,
            int optionNumber,
            long quantity,
            double paidAmount) {
        account.validateExecutedPurchase(eventId, optionNumber, quantity, paidAmount);
    }

    void applyValidatedExecutedPurchase(
            int eventId,
            int optionNumber,
            long quantity,
            double paidAmount) {
        account.applyValidatedExecutedPurchase(eventId, optionNumber, quantity, paidAmount);
    }

    public long getSharesForOption(int eventId, int optionNumber) {
        return account.getSharesForOption(eventId, optionNumber);
    }

    public double getAmountPaidForOption(int eventId, int optionNumber) {
        return account.getAmountPaidForOption(eventId, optionNumber);
    }

    public long getTotalShares(int eventId) {
        return account.getTotalShares(eventId);
    }

    public double getTotalAmountPaid(int eventId) {
        return account.getTotalAmountPaid(eventId);
    }

    public Set<Integer> getPositionEventIds() {
        return account.getPositionEventIds();
    }
}
