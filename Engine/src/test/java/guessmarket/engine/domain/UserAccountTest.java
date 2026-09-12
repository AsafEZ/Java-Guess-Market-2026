package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountTest {
    @Test
    void createsActiveAccountWithPositiveBalance() {
        UserAccount account = new UserAccount(100.0);

        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void rejectsZeroInitialBalance() {
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(0.0));
    }

    @Test
    void rejectsNegativeInitialBalance() {
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(-1.0));
    }

    @Test
    void rejectsNonFiniteInitialBalance() {
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(Double.NEGATIVE_INFINITY));
    }

    @Test
    void creditIncreasesBalance() {
        UserAccount account = new UserAccount(100.0);

        account.credit(25.0);

        assertEquals(125.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void legalDebitKeepsAccountActive() {
        UserAccount account = new UserAccount(100.0);

        account.debit(40.0);

        assertEquals(60.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void debitThatMakesBalanceNegativeBlocksAccount() {
        UserAccount account = new UserAccount(100.0);

        account.debit(125.0);

        assertEquals(-25.0, account.getBalance());
        assertEquals(UserStatus.BLOCKED, account.getStatus());
    }

    @Test
    void blockedAccountRejectsFurtherDebitWithoutChangingState() {
        UserAccount account = blockedAccount();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> account.debit(1.0)
        );

        assertEquals(ErrorCode.USER_ACCOUNT_BLOCKED, exception.getErrorCode());
        assertEquals(-25.0, account.getBalance());
        assertEquals(UserStatus.BLOCKED, account.getStatus());
    }

    @Test
    void blockedAccountAcceptsCreditWithoutBecomingActive() {
        UserAccount account = blockedAccount();

        account.credit(50.0);

        assertEquals(25.0, account.getBalance());
        assertEquals(UserStatus.BLOCKED, account.getStatus());
    }

    @Test
    void canAffordChecksFundsWithoutChangingState() {
        UserAccount account = new UserAccount(100.0);

        assertTrue(account.canAfford(100.0));
        assertTrue(account.canAfford(40.0));
        assertFalse(account.canAfford(100.01));
        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void blockedAccountCannotAffordUserActionEvenAfterCredit() {
        UserAccount account = blockedAccount();
        account.credit(200.0);

        assertFalse(account.canAfford(1.0));
        assertEquals(175.0, account.getBalance());
        assertEquals(UserStatus.BLOCKED, account.getStatus());
    }

    @Test
    void invalidCreditAmountsDoNotChangeState() {
        UserAccount account = new UserAccount(100.0);

        assertThrows(IllegalArgumentException.class, () -> account.credit(0.0));
        assertThrows(IllegalArgumentException.class, () -> account.credit(-1.0));
        assertThrows(IllegalArgumentException.class, () -> account.credit(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> account.credit(Double.POSITIVE_INFINITY));

        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void invalidDebitAmountsDoNotChangeState() {
        UserAccount account = new UserAccount(100.0);

        assertThrows(IllegalArgumentException.class, () -> account.debit(0.0));
        assertThrows(IllegalArgumentException.class, () -> account.debit(-1.0));
        assertThrows(IllegalArgumentException.class, () -> account.debit(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> account.debit(Double.POSITIVE_INFINITY));

        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void invalidCanAffordAmountsDoNotChangeState() {
        UserAccount account = new UserAccount(100.0);

        assertThrows(IllegalArgumentException.class, () -> account.canAfford(0.0));
        assertThrows(IllegalArgumentException.class, () -> account.canAfford(-1.0));
        assertThrows(IllegalArgumentException.class, () -> account.canAfford(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> account.canAfford(Double.POSITIVE_INFINITY));

        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void balanceOverflowIsRejectedWithoutChangingState() {
        UserAccount account = new UserAccount(Double.MAX_VALUE);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> account.credit(Double.MAX_VALUE)
        );

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(Double.MAX_VALUE, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    private static UserAccount blockedAccount() {
        UserAccount account = new UserAccount(100.0);
        account.debit(125.0);
        return account;
    }
}
