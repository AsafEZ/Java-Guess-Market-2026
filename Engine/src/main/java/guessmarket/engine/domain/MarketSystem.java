package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.UserStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MarketSystem {
    private final Map<Integer, MarketEvent> eventsById = new LinkedHashMap<>();
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    public void addUser(User user) {
        Objects.requireNonNull(user, "user");
        if (usersByName.putIfAbsent(user.getName(), user) != null) {
            throw new EngineException(
                    ErrorCode.DUPLICATE_USER_NAME,
                    "Duplicate user name: " + user.getName() + ".");
        }
    }

    public User getUser(String userName) {
        String normalizedName = Objects.requireNonNull(userName, "userName").trim();
        User user = usersByName.get(normalizedName);
        if (user == null) {
            throw new EngineException(
                    ErrorCode.USER_NOT_FOUND,
                    "User '" + normalizedName + "' does not exist.");
        }
        return user;
    }

    public List<User> getAllUsers() {
        return List.copyOf(usersByName.values());
    }

    public void assignMarketMaker(int eventId, String userName) {
        MarketEvent event = getEvent(eventId);
        User user = getUser(userName);
        event.assignMarketMaker(user.getName());
    }

    public synchronized void openEvent(int eventId, String userName) {
        MarketEvent event = getEvent(eventId);
        User user = getUser(userName);

        event.requireCanOpen();
        if (!event.hasMarketMaker()) {
            throw new EngineException(
                    ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                    "Event " + eventId + " does not have a Market Maker.");
        }
        if (!event.isMarketMaker(user.getName())) {
            throw new EngineException(
                    ErrorCode.USER_NOT_MARKET_MAKER,
                    "The user is not authorized to open event " + eventId + ".");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new EngineException(
                    ErrorCode.USER_ACCOUNT_BLOCKED,
                    "A blocked user account cannot open an event.");
        }

        double requiredSubsidy = event.getRequiredInitialSubsidy();
        if (!user.canAfford(requiredSubsidy)) {
            throw new EngineException(
                    ErrorCode.INSUFFICIENT_FUNDS,
                    "The Market Maker cannot afford the required initial funding.");
        }

        event.validateInitialFundingCapacity(requiredSubsidy);
        user.debit(requiredSubsidy);
        try {
            event.openWithFunding(requiredSubsidy);
        } catch (RuntimeException exception) {
            user.credit(requiredSubsidy);
            throw exception;
        }
    }

    public synchronized PurchaseOutcome purchaseShares(
            String userName,
            int eventId,
            int optionNumber,
            long quantity) {
        User buyer = getUser(userName);
        MarketEvent event = getEvent(eventId);

        event.requireActive();
        if (!event.hasMarketMaker()) {
            throw new EngineException(
                    ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                    "Event " + eventId + " does not have a Market Maker.");
        }

        User marketMaker = getUser(event.getMarketMakerName());
        if (buyer.getStatus() == UserStatus.BLOCKED) {
            throw new EngineException(
                    ErrorCode.USER_ACCOUNT_BLOCKED,
                    "A blocked user account cannot purchase shares.");
        }

        PurchaseQuote quote = event.quotePurchase(optionNumber, quantity);
        boolean buyerIsMarketMaker = buyer == marketMaker;
        double buyerDebit = buyerIsMarketMaker
                ? quote.shareCost()
                : quote.totalCharge();

        if (buyerDebit > 0.0) {
            buyer.validateDebit(buyerDebit);
        }
        if (!buyerIsMarketMaker && quote.commission() > 0.0) {
            marketMaker.validateCredit(quote.commission());
        }
        buyer.validateExecutedPurchase(
                eventId,
                optionNumber,
                quantity,
                quote.shareCost(),
                quote.commission());
        MarketEvent.PreparedPurchase preparedPurchase =
                event.prepareUserPurchase(quote, buyer.getName());

        if (buyerDebit > 0.0) {
            buyer.applyValidatedDebit(buyerDebit);
        }
        if (!buyerIsMarketMaker && quote.commission() > 0.0) {
            marketMaker.applyValidatedCredit(quote.commission());
        }
        buyer.applyValidatedExecutedPurchase(
                eventId,
                optionNumber,
                quantity,
                quote.shareCost(),
                quote.commission());
        return event.applyPreparedUserPurchase(preparedPurchase);
    }

    synchronized SettlementPlan prepareSettlement(
            int eventId,
            String actingUserName,
            int winningOptionNumber) {
        MarketEvent event = getEvent(eventId);
        User actingUser = getUser(actingUserName);

        event.requireActive();
        if (!event.hasMarketMaker()) {
            throw new EngineException(
                    ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                    "Event " + eventId + " does not have a Market Maker.");
        }
        if (!event.isMarketMaker(actingUser.getName())) {
            throw new EngineException(
                    ErrorCode.USER_NOT_MARKET_MAKER,
                    "The user is not authorized to close event " + eventId + ".");
        }
        if (actingUser.getStatus() == UserStatus.BLOCKED) {
            throw new EngineException(
                    ErrorCode.USER_ACCOUNT_BLOCKED,
                    "A blocked Market Maker cannot close an event.");
        }

        event.findOption(winningOptionNumber);
        validatePositionAggregates(event);

        double payoutPerWinningShare = requirePositiveFinite(
                event.getPayoutPerWinningShare(),
                "The winning-share payout must be a positive finite value.");
        List<UserSettlement> userSettlements = new ArrayList<>();
        double totalGrossPayout = 0.0;
        double totalClosingCommission = 0.0;
        double totalWinnerNetPayout = 0.0;

        for (User user : usersByName.values()) {
            long winningShares = user.getSharesForOption(eventId, winningOptionNumber);
            if (winningShares == 0L) {
                continue;
            }

            double grossPayout = requireFiniteNonNegative(
                    winningShares * payoutPerWinningShare,
                    "A winner's gross payout exceeds the supported numeric range.");
            double closingCommission = event.getCommissionPolicy().type()
                    == CommissionType.ON_CLOSE
                    ? requireFiniteNonNegative(
                            event.getCommissionPolicy().calculate(grossPayout),
                            "A winner's closing commission exceeds the supported numeric range.")
                    : 0.0;
            double netPayout = requireFiniteNonNegative(
                    grossPayout - closingCommission,
                    "A winner's net payout exceeds the supported numeric range.");

            userSettlements.add(new UserSettlement(
                    user.getName(),
                    winningShares,
                    grossPayout,
                    closingCommission,
                    netPayout));
            totalGrossPayout = addFinite(totalGrossPayout, grossPayout);
            totalClosingCommission = addFinite(
                    totalClosingCommission, closingCommission);
            totalWinnerNetPayout = addFinite(totalWinnerNetPayout, netPayout);
        }

        double eventBalanceBefore = requireFiniteNonNegative(
                event.getAccount().getBalance(),
                "The event account balance must be finite and non-negative.");
        if (eventBalanceBefore < totalGrossPayout) {
            throw new EngineException(
                    ErrorCode.INSUFFICIENT_EVENT_FUNDS,
                    "Event " + eventId + " cannot cover the complete settlement.");
        }

        double marketMakerResidual = requireFiniteNonNegative(
                eventBalanceBefore - totalGrossPayout,
                "The Market Maker residual exceeds the supported numeric range.");
        double totalMarketMakerCredit = addFinite(
                totalClosingCommission, marketMakerResidual);
        List<AccountCredit> accountCredits = consolidateAccountCredits(
                userSettlements,
                actingUser.getName(),
                totalMarketMakerCredit);
        requireConservation(
                eventBalanceBefore,
                totalWinnerNetPayout,
                totalClosingCommission,
                marketMakerResidual);

        return new SettlementPlan(
                eventId,
                winningOptionNumber,
                actingUser.getName(),
                eventBalanceBefore,
                payoutPerWinningShare,
                userSettlements,
                accountCredits,
                totalGrossPayout,
                totalClosingCommission,
                totalWinnerNetPayout,
                marketMakerResidual,
                totalMarketMakerCredit);
    }

    private void validatePositionAggregates(MarketEvent event) {
        for (MarketOption option : event.getOptions()) {
            long positionShares = 0L;
            for (User user : usersByName.values()) {
                try {
                    positionShares = Math.addExact(
                            positionShares,
                            user.getSharesForOption(event.getId(), option.getOptionNumber()));
                } catch (ArithmeticException exception) {
                    throw new EngineException(
                            ErrorCode.ARITHMETIC_OVERFLOW,
                            "User position shares exceed the supported range.",
                            exception);
                }
            }
            if (positionShares != option.getPurchasedShares()) {
                throw new EngineException(
                        ErrorCode.POSITION_AGGREGATE_MISMATCH,
                        "User positions do not match aggregate shares for event "
                                + event.getId()
                                + ", option "
                                + option.getOptionNumber()
                                + ".");
            }
        }
    }

    private static List<AccountCredit> consolidateAccountCredits(
            List<UserSettlement> settlements,
            String marketMakerName,
            double totalMarketMakerCredit) {
        Map<String, Double> creditsByUserName = new LinkedHashMap<>();
        for (UserSettlement settlement : settlements) {
            if (settlement.netPayout() > 0.0) {
                creditsByUserName.put(
                        settlement.userName(),
                        settlement.netPayout());
            }
        }
        if (totalMarketMakerCredit > 0.0) {
            creditsByUserName.merge(
                    marketMakerName,
                    totalMarketMakerCredit,
                    MarketSystem::addFinite);
        }

        return creditsByUserName.entrySet().stream()
                .map(entry -> new AccountCredit(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static double addFinite(double first, double second) {
        return requireFiniteNonNegative(
                first + second,
                "A settlement total exceeds the supported numeric range.");
    }

    private static double requirePositiveFinite(double value, String message) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new EngineException(ErrorCode.ARITHMETIC_OVERFLOW, message);
        }
        return value;
    }

    private static double requireFiniteNonNegative(double value, String message) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new EngineException(ErrorCode.ARITHMETIC_OVERFLOW, message);
        }
        return value;
    }

    private static void requireConservation(
            double eventBalance,
            double winnerNetPayout,
            double closingCommission,
            double marketMakerResidual) {
        double plannedTotal = addFinite(
                addFinite(winnerNetPayout, closingCommission),
                marketMakerResidual);
        double tolerance = Math.ulp(Math.max(1.0, Math.abs(eventBalance))) * 4.0;
        if (Math.abs(plannedTotal - eventBalance) > tolerance) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "Settlement credits do not conserve the event account balance.");
        }
    }

    public void addEvent(MarketEvent event) {
        if (eventsById.putIfAbsent(event.getId(), event) != null) {
            throw new EngineException(
                    ErrorCode.DUPLICATE_EVENT_ID,
                    "Duplicate event id: " + event.getId() + ".");
        }
    }

    public MarketEvent getEvent(int eventId) {
        MarketEvent event = eventsById.get(eventId);
        if (event == null) {
            throw new EngineException(
                    ErrorCode.EVENT_NOT_FOUND,
                    "Event " + eventId + " does not exist.");
        }
        return event;
    }

    public List<MarketEvent> getAllEvents() {
        return List.copyOf(eventsById.values());
    }

    public List<MarketEvent> getActiveEvents() {
        List<MarketEvent> active = new ArrayList<>();
        for (MarketEvent event : eventsById.values()) {
            if (event.getStatus() == guessmarket.engine.enums.EventStatus.ACTIVE) {
                active.add(event);
            }
        }
        return List.copyOf(active);
    }

    public int size() {
        return eventsById.size();
    }

    public double totalInitialSubsidy() {
        double total = 0.0;
        for (MarketEvent event : eventsById.values()) {
            total += event.getAccount().getBalance();
        }
        return total;
    }
}

