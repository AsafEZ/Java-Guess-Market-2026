package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {
    @Test
    void createsValidUser() {
        User user = new User("Alice", 100.0);

        assertEquals("Alice", user.getName());
        assertEquals(100.0, user.getBalance());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void trimsUserName() {
        User user = new User("  Alice  ", 100.0);

        assertEquals("Alice", user.getName());
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class, () -> new User(null, 100.0));
    }

    @Test
    void rejectsEmptyOrBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new User("", 100.0));
        assertThrows(IllegalArgumentException.class, () -> new User("   ", 100.0));
    }

    @Test
    void createsAccountWithInitialBalance() {
        User user = new User("Alice", 275.5);

        assertEquals(275.5, user.getBalance());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void delegatesAccountOperations() {
        User user = new User("Alice", 100.0);

        user.credit(25.0);
        assertEquals(125.0, user.getBalance());
        assertTrue(user.canAfford(125.0));

        user.debit(150.0);
        assertEquals(-25.0, user.getBalance());
        assertEquals(UserStatus.BLOCKED, user.getStatus());

        user.credit(50.0);
        assertEquals(25.0, user.getBalance());
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertFalse(user.canAfford(1.0));
    }

    @Test
    void doesNotDuplicateOrExposeAccountState() {
        Field[] fields = User.class.getDeclaredFields();

        assertEquals(1, Arrays.stream(fields)
                .filter(field -> field.getType().equals(UserAccount.class))
                .count());
        assertFalse(Arrays.stream(fields)
                .anyMatch(field -> field.getType().equals(double.class)
                        || field.getType().equals(Double.class)
                        || field.getType().equals(UserStatus.class)));
        assertThrows(NoSuchMethodException.class, () -> User.class.getMethod("getAccount"));
    }

    @Test
    void delegatesPositionBookkeepingAndQueries() {
        User user = new User("Alice", 100.0);

        user.recordExecutedPurchase(7, 1, 4L, 12.5, 1.25);
        user.recordExecutedPurchase(7, 2, 6L, 21.0, 2.1);

        assertTrue(user.hasPosition(7));
        assertEquals(4L, user.getSharesForOption(7, 1));
        assertEquals(12.5, user.getAmountPaidForOption(7, 1));
        assertEquals(1.25, user.getCommissionPaidForOption(7, 1));
        assertEquals(10L, user.getTotalShares(7));
        assertEquals(33.5, user.getTotalAmountPaid(7));
        assertEquals(3.35, user.getTotalCommissionPaid(7), 1.0e-12);
        assertEquals(Set.of(7), user.getPositionEventIds());
        assertEquals(100.0, user.getBalance());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }
}
