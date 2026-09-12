package guessmarket.engine.domain;

import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingOperations;
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
    private EventStatus status;
    private Integer winningOptionNumber;
    private long nextTradeNumber = 1;
    private final TradingMechanism tradingMechanism;
    private String marketMakerName;

    public MarketEvent(
            int id,
            String name,
            String description,
            List<MarketOption> options,
            CommissionPolicy commissionPolicy,
            TradingMechanism tradingMechanism,
            double initialSubsidy) {
        this(
                id,
                name,
                description,
                options,
                commissionPolicy,
                tradingMechanism,
                initialSubsidy,
                EventStatus.ACTIVE);
    }

    public static MarketEvent createNotStartedEvent(
            int id,
            String name,
            String description,
            List<MarketOption> options,
            CommissionPolicy commissionPolicy,
            TradingMechanism tradingMechanism) {
        return new MarketEvent(
                id,
                name,
                description,
                options,
                commissionPolicy,
                tradingMechanism,
                0.0,
                EventStatus.NOT_STARTED);
    }

    private MarketEvent(
            int id,
            String name,
            String description,
            List<MarketOption> options,
            CommissionPolicy commissionPolicy,
            TradingMechanism tradingMechanism,
            double initialAccountBalance,
            EventStatus initialStatus) {
        this.tradingMechanism =
                Objects.requireNonNull(tradingMechanism, "tradingMechanism");
        Objects.requireNonNull(options, "options");
        if (options.size() != 2) {
            throw new IllegalArgumentException("Exercise 1 requires exactly two options.");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.options = List.copyOf(options);
        this.commissionPolicy = Objects.requireNonNull(commissionPolicy, "commissionPolicy");
        this.account = new EventAccount(initialAccountBalance);
        this.status = Objects.requireNonNull(initialStatus, "initialStatus");
    }

    public PurchaseOutcome purchase(
            int optionNumber,
            long quantity) {

        requireActive();

        if (quantity <= 0) {
            throw new EngineException(
                    ErrorCode.INVALID_SHARE_QUANTITY,
                    "Share quantity must be a positive whole number."
            );
        }

        MarketOption selected = findOption(optionNumber);

        LmsrTradingOperations lmsrOperations =
                requireLmsrOperations();

        double shareCost = lmsrOperations.executePurchase(
                options,
                selected,
                quantity
        );

        double commission =
                commissionPolicy.type() == CommissionType.ON_PURCHASE
                        ? commissionPolicy.calculate(shareCost)
                        : 0.0;

        double totalPaid = shareCost + commission;

        account.recordPurchase(shareCost, commission);

        trades.add(new Trade(
                nextTradeNumber++,
                optionNumber,
                selected.getName(),
                quantity,
                shareCost,
                commission,
                totalPaid
        ));

        return new PurchaseOutcome(
                optionNumber,
                quantity,
                shareCost,
                commission,
                totalPaid
        );
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

    public double getLmsrOptionValue(int optionNumber) {
        findOption(optionNumber);

        return requireLmsrOperations()
                .calculateOptionValue(
                        options,
                        optionNumber
                );
    }

    private void requireActive() {
        if (status == EventStatus.NOT_STARTED) {
            throw new EngineException(
                    ErrorCode.EVENT_NOT_STARTED,
                    "Event " + id + " has not started.");
        }
        if (status == EventStatus.CLOSED) {
            throw new EngineException(
                    ErrorCode.EVENT_ALREADY_CLOSED,
                    "Event " + id + " is already closed.");
        }
    }

    private LmsrTradingOperations requireLmsrOperations() {
        if (tradingMechanism instanceof LmsrTradingOperations lmsrOperations) {

            return lmsrOperations;
        }

        throw new EngineException(
                ErrorCode.WRONG_TRADING_METHOD,
                "Event " + id + " does not use LMSR."
        );
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
        return requireLmsrOperations().getB();
    }

    public double getRequiredInitialSubsidy() {
        return requireLmsrOperations().calculateInitialSubsidy();
    }

    void requireCanOpen() {
        if (status == EventStatus.ACTIVE) {
            throw new EngineException(
                    ErrorCode.EVENT_ALREADY_STARTED,
                    "Event " + id + " is already active.");
        }
        if (status == EventStatus.CLOSED) {
            throw new EngineException(
                    ErrorCode.EVENT_ALREADY_CLOSED,
                    "Event " + id + " is already closed.");
        }
    }

    void validateInitialFundingCapacity(double amount) {
        requireCanOpen();
        account.validateCredit(amount);
    }

    void openWithFunding(double amount) {
        requireCanOpen();
        double requiredSubsidy = getRequiredInitialSubsidy();
        if (Double.compare(amount, requiredSubsidy) != 0) {
            throw new IllegalStateException(
                    "Initial funding no longer matches the required LMSR subsidy.");
        }

        account.credit(amount);
        status = EventStatus.ACTIVE;
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

    public boolean hasMarketMaker() {
        return marketMakerName != null;
    }

    void assignMarketMaker(String userName) {
        if (hasMarketMaker()) {
            throw new EngineException(
                    ErrorCode.MARKET_MAKER_ALREADY_ASSIGNED,
                    "Event " + id + " already has a Market Maker.");
        }
        marketMakerName = normalizeUserName(userName);
    }

    public String getMarketMakerName() {
        if (!hasMarketMaker()) {
            throw new EngineException(
                    ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                    "Event " + id + " does not have a Market Maker.");
        }
        return marketMakerName;
    }

    public boolean isMarketMaker(String userName) {
        return normalizeUserName(userName).equals(marketMakerName);
    }

    private static String normalizeUserName(String userName) {
        String normalizedName = Objects.requireNonNull(userName, "userName").trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }
        return normalizedName;
    }

    public TradingMechanism getTradingMechanism() {return tradingMechanism;}

    public TradingMethod getTradingMethod() {return tradingMechanism.getTradingMethod();}


}
