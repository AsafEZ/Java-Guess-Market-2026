package guessmarket.engine.domain;

import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MarketEvent {
    private final int id;
    private final String name;
    private final String description;
    private final List<MarketOption> options;
    private final CommissionPolicy commissionPolicy;
    private final EventAccount account;
    private final List<Trade> trades = new ArrayList<>();
    private EventStatus status = EventStatus.ACTIVE;
    private Integer winningOptionNumber;
    private long nextTradeNumber = 1;
    private final TradingMechanism tradingMechanism;

    public MarketEvent(
            int id,
            String name,
            String description,
            List<MarketOption> options,
            CommissionPolicy commissionPolicy,
            int b,
            double initialSubsidy, TradingMechanism tradingMechanism) {
        this.tradingMechanism =
                Objects.requireNonNull(tradingMechanism, "tradingMechanism");
        if (options.size() != 2) {
            throw new IllegalArgumentException("Exercise 1 requires exactly two options.");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.options = List.copyOf(options);
        this.commissionPolicy = Objects.requireNonNull(commissionPolicy, "commissionPolicy");
        this.b = b;
        this.account = new EventAccount(initialSubsidy);
    }

    public PurchaseOutcome purchase(int optionNumber, long quantity, LmsrCalculator calculator) {
        requireActive();
        if (quantity <= 0) {
            throw new EngineException(
                    ErrorCode.INVALID_SHARE_QUANTITY,
                    "Share quantity must be a positive whole number.");
        }

        MarketOption selected = findOption(optionNumber);
        int optionIndex = selected.getOptionNumber() - 1;
        long firstShares = options.get(0).getPurchasedShares();
        long secondShares = options.get(1).getPurchasedShares();

        final double shareCost;
        try {
            shareCost = calculator.purchaseCost(
                    b, firstShares, secondShares, optionIndex, quantity);
            selected.addShares(quantity);
        } catch (ArithmeticException ex) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The requested purchase is too large.",
                    ex);
        }

        double commission = commissionPolicy.type() == CommissionType.ON_PURCHASE
                ? commissionPolicy.calculate(shareCost)
                : 0.0;
        double totalPaid = shareCost + commission;
        account.recordPurchase(shareCost, commission);
        trades.add(new Trade(
                nextTradeNumber++, optionNumber, selected.getName(), quantity,
                shareCost, commission, totalPaid));

        return new PurchaseOutcome(optionNumber, quantity, shareCost, commission, totalPaid);
    }

    public CloseOutcome close(int optionNumber) {
        requireActive();
        MarketOption winner = findOption(optionNumber);
        double grossPayout = winner.getPurchasedShares();
        double commission = commissionPolicy.type() == CommissionType.ON_CLOSE
                ? commissionPolicy.calculate(grossPayout)
                : 0.0;
        double netPayout = grossPayout - commission;

        account.settleClosedEvent(grossPayout, commission);
        status = EventStatus.CLOSED;
        winningOptionNumber = optionNumber;

        return new CloseOutcome(
                optionNumber, winner.getName(), grossPayout, commission, netPayout);
    }

    public MarketOption findOption(int optionNumber) {
        if (optionNumber < 1 || optionNumber > options.size()) {
            throw new EngineException(
                    ErrorCode.INVALID_OPTION_NUMBER,
                    "Option number " + optionNumber + " is invalid for event " + id + ".");
        }
        return options.get(optionNumber - 1);
    }

    private void requireActive() {
        if (status == EventStatus.CLOSED) {
            throw new EngineException(
                    ErrorCode.EVENT_ALREADY_CLOSED,
                    "Event " + id + " is already closed.");
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<MarketOption> getOptions() {
        return options;
    }

    public CommissionPolicy getCommissionPolicy() {
        return commissionPolicy;
    }

    public int getB() {
        return b;
    }

    public EventAccount getAccount() {
        return account;
    }

    public List<Trade> getTrades() {
        return List.copyOf(trades);
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getWinningOptionNumber() {
        return winningOptionNumber;
    }

    public TradingMechanism getTradingMechanism() {return tradingMechanism;}

    public TradingMethod getTradingMethod() {return tradingMechanism.getTradingMethod();}
}