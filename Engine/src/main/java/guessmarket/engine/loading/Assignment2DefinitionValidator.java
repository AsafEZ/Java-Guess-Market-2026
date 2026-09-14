package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Assignment2DefinitionValidator {

    void validate(Assignment2Definition definition) {
        Map<Integer, Integer> marketMakerCounts = new HashMap<>();
        Set<Integer> eventIds = validateEvents(definition);
        validateUsers(definition, eventIds, marketMakerCounts);

        for (int eventId : eventIds) {
            int count = marketMakerCounts.getOrDefault(eventId, 0);
            if (count == 0) {
                throw new EngineException(
                        ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                        "Event " + eventId + " does not have a Market Maker.");
            }
            if (count > 1) {
                throw new EngineException(
                        ErrorCode.MARKET_MAKER_ALREADY_ASSIGNED,
                        "Event " + eventId + " has more than one Market Maker.");
            }
        }
    }

    private static Set<Integer> validateEvents(Assignment2Definition definition) {
        Set<Integer> eventIds = new HashSet<>();
        for (Assignment2EventDefinition event : definition.events()) {
            if (!eventIds.add(event.id())) {
                throw new EngineException(
                        ErrorCode.DUPLICATE_EVENT_ID,
                        "Event id " + event.id() + " appears more than once.");
            }
            requireNonBlank(
                    event.name(),
                    ErrorCode.INVALID_EVENT_DEFINITION,
                    "Event " + event.id() + " has a blank name.");
            requireNonBlank(
                    event.description(),
                    ErrorCode.INVALID_EVENT_DEFINITION,
                    "Event " + event.id() + " has a blank description.");
            validateCommission(event);
            validateOptions(event);
            validateMechanism(event);
        }
        return eventIds;
    }

    private static void validateUsers(
            Assignment2Definition definition,
            Set<Integer> eventIds,
            Map<Integer, Integer> marketMakerCounts) {
        Set<String> userNames = new HashSet<>();
        for (UserDefinition user : definition.users()) {
            String normalizedName = requireNonBlank(
                    user.name(),
                    ErrorCode.INVALID_USER_NAME,
                    "User name cannot be null or blank.");
            if (!userNames.add(normalizedName)) {
                throw new EngineException(
                        ErrorCode.DUPLICATE_USER_NAME,
                        "Duplicate user name: " + normalizedName + ".");
            }
            if (user.initialCash() <= 0) {
                throw new EngineException(
                        ErrorCode.INVALID_INITIAL_CASH,
                        "User '" + normalizedName
                                + "' must have initial cash greater than zero.");
            }
            for (int eventId : user.marketMakerEventIds()) {
                if (!eventIds.contains(eventId)) {
                    throw new EngineException(
                            ErrorCode.EVENT_NOT_FOUND,
                            "Market Maker '" + normalizedName
                                    + "' refers to missing event " + eventId + ".");
                }
                marketMakerCounts.merge(eventId, 1, Integer::sum);
            }
        }
    }

    private static void validateCommission(Assignment2EventDefinition event) {
        int percentage = event.commission().percentage();
        if (percentage < 0 || percentage > 90) {
            throw new EngineException(
                    ErrorCode.INVALID_COMMISSION,
                    "Event " + event.id() + " has commission " + percentage
                            + "; expected 0 to 90.");
        }
    }

    private static void validateOptions(Assignment2EventDefinition event) {
        if (event.options().size() != 2) {
            throw new EngineException(
                    ErrorCode.INVALID_OPTION_COUNT,
                    "Event " + event.id() + " must contain exactly two options.");
        }
        Set<String> names = new HashSet<>();
        for (OptionDefinition option : event.options()) {
            String normalizedName = requireNonBlank(
                    option.name(),
                    ErrorCode.INVALID_EVENT_DEFINITION,
                    "Event " + event.id() + " has a blank option name.");
            if (!names.add(normalizedName)) {
                throw new EngineException(
                        ErrorCode.INVALID_EVENT_DEFINITION,
                        "Event " + event.id() + " contains duplicate option name '"
                                + normalizedName + "'.");
            }
        }
    }

    private static void validateMechanism(Assignment2EventDefinition event) {
        if (event.tradingMechanism() instanceof LmsrDefinition lmsr) {
            if (lmsr.b() <= 0) {
                throw new EngineException(
                        ErrorCode.INVALID_LMSR_B,
                        "Event " + event.id() + " has LMSR b=" + lmsr.b()
                                + "; b must be positive.");
            }
            return;
        }
        if (event.tradingMechanism() instanceof OrderBookDefinition orderBook) {
            if (orderBook.initial() < 0) {
                throw new EngineException(
                        ErrorCode.INVALID_ORDER_BOOK_INITIAL,
                        "Event " + event.id()
                                + " has a negative Order Book initial investment.");
            }
            if (orderBook.d() <= 0) {
                throw new EngineException(
                        ErrorCode.INVALID_ORDER_BOOK_D,
                        "Event " + event.id() + " has Order Book d="
                                + orderBook.d() + "; d must be positive.");
            }
            if (orderBook.initial() % orderBook.d() != 0) {
                throw new EngineException(
                        ErrorCode.INVALID_ORDER_BOOK_INITIAL,
                        "Event " + event.id()
                                + " has an initial investment that is not divisible by d.");
            }
            return;
        }
        throw new EngineException(
                ErrorCode.WRONG_TRADING_METHOD,
                "Event " + event.id() + " has an unsupported trading method.");
    }

    private static String requireNonBlank(
            String value,
            ErrorCode errorCode,
            String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new EngineException(errorCode, message);
        }
        return value.trim();
    }
}
