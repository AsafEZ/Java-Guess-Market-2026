package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
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

