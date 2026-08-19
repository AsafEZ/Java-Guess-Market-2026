package guessmarket.engine.domain;


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