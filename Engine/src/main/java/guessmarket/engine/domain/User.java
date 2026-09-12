package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;

import java.util.Objects;

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
}
