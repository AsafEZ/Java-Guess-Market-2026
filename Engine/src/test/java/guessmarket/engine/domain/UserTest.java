package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

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
}
