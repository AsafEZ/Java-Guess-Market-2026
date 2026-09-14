package guessmarket.engine.domain;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketMakerAssignmentTest {
    @Test
    void legacyEventWorksWithoutMarketMaker() {
        MarketEvent event = createEvent(7);

        assertFalse(event.hasMarketMaker());
        EngineException exception = assertThrows(
                EngineException.class,
                event::getMarketMakerName);
        assertEquals(ErrorCode.MARKET_MAKER_NOT_ASSIGNED, exception.getErrorCode());

        PurchaseOutcome purchase = event.purchase(1, 1L);
        assertEquals(1L, purchase.shareQuantity());
        CloseOutcome close = event.close(1);
        assertEquals(1, close.winningOptionNumber());
        assertEquals(EventStatus.CLOSED, event.getStatus());
    }

    @Test
    void assignsExistingUserToExistingEvent() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);

        system.assignMarketMaker(7, "Alice");

        assertTrue(system.getEvent(7).hasMarketMaker());
    }

    @Test
    void returnsCanonicalMarketMakerNameAfterAssignment() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);

        system.assignMarketMaker(7, "Alice");

        assertEquals("Alice", system.getEvent(7).getMarketMakerName());
    }

    @Test
    void identifiesMarketMakerByName() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        system.assignMarketMaker(7, "Alice");
        MarketEvent event = system.getEvent(7);

        assertTrue(event.isMarketMaker("Alice"));
        assertFalse(event.isMarketMaker("Bob"));
    }

    @Test
    void rejectsUnknownUser() {
        MarketSystem system = new MarketSystem();
        MarketEvent event = createEvent(7);
        system.addEvent(event);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.assignMarketMaker(7, "Missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertFalse(event.hasMarketMaker());
    }

    @Test
    void rejectsUnknownEvent() {
        MarketSystem system = new MarketSystem();
        system.addUser(new User("Alice", 100.0));

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.assignMarketMaker(999, "Alice"));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsDifferentSecondMarketMakerWithoutChangingAssignment() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        system.addUser(new User("Bob", 100.0));
        system.assignMarketMaker(7, "Alice");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.assignMarketMaker(7, "Bob"));

        assertEquals(ErrorCode.MARKET_MAKER_ALREADY_ASSIGNED, exception.getErrorCode());
        assertEquals("Alice", system.getEvent(7).getMarketMakerName());
    }

    @Test
    void rejectsRepeatedAssignmentOfSameUserWithoutChangingState() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        system.assignMarketMaker(7, "Alice");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.assignMarketMaker(7, "Alice"));

        assertEquals(ErrorCode.MARKET_MAKER_ALREADY_ASSIGNED, exception.getErrorCode());
        assertEquals("Alice", system.getEvent(7).getMarketMakerName());
    }

    @Test
    void comparesMarketMakerNamesCaseSensitively() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        system.addUser(new User("alice", 100.0));
        system.assignMarketMaker(7, "Alice");
        MarketEvent event = system.getEvent(7);

        assertTrue(event.isMarketMaker("Alice"));
        assertFalse(event.isMarketMaker("alice"));
    }

    @Test
    void trimsLookupAndStoresCanonicalUserName() {
        MarketSystem system = systemWithUserAndEvent("  Alice  ", 7);

        system.assignMarketMaker(7, " Alice ");
        MarketEvent event = system.getEvent(7);

        assertEquals("Alice", event.getMarketMakerName());
        assertTrue(event.isMarketMaker("  Alice  "));
    }

    @Test
    void assignmentDoesNotChangeUserAccountState() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        User user = system.getUser("Alice");

        system.assignMarketMaker(7, "Alice");

        assertEquals(100.0, user.getBalance());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getPositionEventIds().isEmpty());
    }

    @Test
    void existingUserAndEventManagementRemainUnchanged() {
        MarketSystem system = systemWithUserAndEvent("Alice", 7);
        User bob = new User("Bob", 200.0);
        MarketEvent secondEvent = createEvent(8);
        system.addUser(bob);
        system.addEvent(secondEvent);

        system.assignMarketMaker(7, "Alice");

        assertEquals(2, system.size());
        assertEquals(2, system.getAllEvents().size());
        assertEquals(2, system.getAllUsers().size());
        assertSame(bob, system.getUser("Bob"));
        assertSame(secondEvent, system.getEvent(8));
        assertFalse(secondEvent.hasMarketMaker());
    }

    @Test
    void eventStoresMarketMakerNameWithoutUserReference() {
        assertFalse(Arrays.stream(MarketEvent.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(User.class::equals));
    }

    private static MarketSystem systemWithUserAndEvent(String userName, int eventId) {
        MarketSystem system = new MarketSystem();
        system.addUser(new User(userName, 100.0));
        system.addEvent(createEvent(eventId));
        return system;
    }

    private static MarketEvent createEvent(int eventId) {
        LmsrTradingMechanism mechanism = new LmsrTradingMechanism(
                10,
                new LmsrCalculator());
        return new MarketEvent(
                eventId,
                "Event " + eventId,
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                mechanism,
                mechanism.calculateInitialSubsidy());
    }
}
