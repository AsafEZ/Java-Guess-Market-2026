package guessmarket.engine.domain;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.TradingMechanism;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSystemTest {
    @Test
    void newSystemStartsWithEmptyUserRegistry() {
        MarketSystem system = new MarketSystem();

        assertTrue(system.getAllUsers().isEmpty());
    }

    @Test
    void addsAndFindsUserByName() {
        MarketSystem system = new MarketSystem();
        User user = new User("Alice", 100.0);

        system.addUser(user);

        assertSame(user, system.getUser("Alice"));
        assertEquals(List.of(user), system.getAllUsers());
    }

    @Test
    void rejectsDuplicateUserName() {
        MarketSystem system = new MarketSystem();
        User original = new User("Alice", 100.0);
        system.addUser(original);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.addUser(new User("Alice", 200.0)));

        assertEquals(ErrorCode.DUPLICATE_USER_NAME, exception.getErrorCode());
        assertSame(original, system.getUser("Alice"));
        assertEquals(1, system.getAllUsers().size());
    }

    @Test
    void trimsNamesForRegistrationAndLookup() {
        MarketSystem system = new MarketSystem();
        User user = new User("  Alice  ", 100.0);
        system.addUser(user);

        assertSame(user, system.getUser(" Alice "));

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.addUser(new User("Alice", 200.0)));
        assertEquals(ErrorCode.DUPLICATE_USER_NAME, exception.getErrorCode());
    }

    @Test
    void treatsNamesWithDifferentCaseAsDistinct() {
        MarketSystem system = new MarketSystem();
        User upperCaseUser = new User("Alice", 100.0);
        User lowerCaseUser = new User("alice", 200.0);

        system.addUser(upperCaseUser);
        system.addUser(lowerCaseUser);

        assertSame(upperCaseUser, system.getUser("Alice"));
        assertSame(lowerCaseUser, system.getUser("alice"));
        assertEquals(2, system.getAllUsers().size());
    }

    @Test
    void throwsForMissingUser() {
        MarketSystem system = new MarketSystem();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.getUser("Missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void returnedUsersCannotModifyRegistry() {
        MarketSystem system = new MarketSystem();
        system.addUser(new User("Alice", 100.0));

        List<User> users = system.getAllUsers();

        assertThrows(
                UnsupportedOperationException.class,
                () -> users.add(new User("Bob", 100.0)));
        assertEquals(1, system.getAllUsers().size());
    }

    @Test
    void preservesUserInsertionOrder() {
        MarketSystem system = new MarketSystem();
        User first = new User("Charlie", 100.0);
        User second = new User("Alice", 100.0);
        User third = new User("Bob", 100.0);

        system.addUser(first);
        system.addUser(second);
        system.addUser(third);

        assertEquals(List.of(first, second, third), system.getAllUsers());
    }

    @Test
    void existingEventManagementRemainsUnchanged() {
        MarketSystem system = new MarketSystem();
        MarketEvent event = createEvent(7);

        system.addUser(new User("Alice", 100.0));
        system.addEvent(event);

        assertSame(event, system.getEvent(7));
        assertEquals(List.of(event), system.getAllEvents());
        assertEquals(List.of(event), system.getActiveEvents());
        assertEquals(1, system.size());
    }

    private static MarketEvent createEvent(int eventId) {
        TradingMechanism mechanism = () -> TradingMethod.LMSR;
        return new MarketEvent(
                eventId,
                "Event " + eventId,
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                mechanism,
                10.0);
    }
}
