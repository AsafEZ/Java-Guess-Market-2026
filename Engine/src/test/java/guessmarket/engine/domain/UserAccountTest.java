package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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

    @Test
    void newAccountHasNoPositions() {
        UserAccount account = new UserAccount(100.0);

        assertFalse(account.hasPosition(1));
        assertTrue(account.getPositionEventIds().isEmpty());
    }

    @Test
    void firstExecutedPurchaseCreatesPosition() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(7, 1, 4L, 12.5, 1.25);

        assertTrue(account.hasPosition(7));
        assertEquals(4L, account.getSharesForOption(7, 1));
        assertEquals(12.5, account.getAmountPaidForOption(7, 1));
        assertEquals(1.25, account.getCommissionPaidForOption(7, 1));
    }

    @Test
    void accumulatesPurchasesInSameEventAndOption() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(7, 1, 4L, 12.5);
        account.recordExecutedPurchase(7, 1, 3L, 8.25);

        assertEquals(7L, account.getSharesForOption(7, 1));
        assertEquals(20.75, account.getAmountPaidForOption(7, 1));
    }

    @Test
    void tracksMultipleOptionsWithinOneEvent() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(7, 1, 4L, 12.5, 1.25);
        account.recordExecutedPurchase(7, 2, 6L, 21.0, 2.1);

        assertEquals(4L, account.getSharesForOption(7, 1));
        assertEquals(6L, account.getSharesForOption(7, 2));
        assertEquals(1.25, account.getCommissionPaidForOption(7, 1));
        assertEquals(2.1, account.getCommissionPaidForOption(7, 2));
        assertEquals(10L, account.getTotalShares(7));
        assertEquals(33.5, account.getTotalAmountPaid(7));
        assertEquals(3.35, account.getTotalCommissionPaid(7), 1.0e-12);
    }

    @Test
    void keepsPositionsForDifferentEventsSeparate() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(7, 1, 4L, 12.5);
        account.recordExecutedPurchase(8, 1, 6L, 21.0);

        assertEquals(4L, account.getSharesForOption(7, 1));
        assertEquals(12.5, account.getAmountPaidForOption(7, 1));
        assertEquals(6L, account.getSharesForOption(8, 1));
        assertEquals(21.0, account.getAmountPaidForOption(8, 1));
        assertEquals(Set.of(7, 8), account.getPositionEventIds());
    }

    @Test
    void missingPositionQueriesReturnZero() {
        UserAccount account = new UserAccount(100.0);

        assertEquals(0L, account.getSharesForOption(7, 1));
        assertEquals(0.0, account.getAmountPaidForOption(7, 1));
        assertEquals(0.0, account.getCommissionPaidForOption(7, 1));
        assertEquals(0L, account.getTotalShares(7));
        assertEquals(0.0, account.getTotalAmountPaid(7));
        assertEquals(0.0, account.getTotalCommissionPaid(7));
    }

    @Test
    void positionEventIdsAreAnImmutableSnapshot() {
        UserAccount account = new UserAccount(100.0);
        account.recordExecutedPurchase(7, 1, 1L, 2.0);
        Set<Integer> eventIds = account.getPositionEventIds();

        assertThrows(UnsupportedOperationException.class, () -> eventIds.add(8));

        account.recordExecutedPurchase(8, 1, 1L, 3.0);
        assertEquals(Set.of(7), eventIds);
        assertEquals(Set.of(7, 8), account.getPositionEventIds());
    }

    @Test
    void invalidPurchaseDoesNotCreateEmptyPosition() {
        UserAccount account = new UserAccount(100.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(0, 1, 1L, 2.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(7, 0, 1L, 2.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(8, 1, 0L, 2.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(9, 1, 1L, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(10, 1, 1L, 2.0, Double.NaN));

        assertTrue(account.getPositionEventIds().isEmpty());
    }

    @Test
    void failedAdditionalPurchaseDoesNotChangeExistingPosition() {
        UserAccount account = new UserAccount(100.0);
        account.recordExecutedPurchase(7, 1, 4L, 12.5, 1.25);

        assertThrows(
                IllegalArgumentException.class,
                () -> account.recordExecutedPurchase(7, 1, -1L, 5.0, 0.5));

        assertEquals(4L, account.getSharesForOption(7, 1));
        assertEquals(12.5, account.getAmountPaidForOption(7, 1));
        assertEquals(1.25, account.getCommissionPaidForOption(7, 1));
        assertEquals(Set.of(7), account.getPositionEventIds());
    }

    @Test
    void positionBookkeepingDoesNotChangeBalanceOrStatus() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(7, 1, 4L, 12.5);

        assertEquals(100.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void doesNotExposeMutablePositionOrMap() {
        Method[] methods = UserAccount.class.getMethods();

        assertFalse(Arrays.stream(methods)
                .map(Method::getReturnType)
                .anyMatch(returnType -> Map.class.isAssignableFrom(returnType)
                        || MarketPosition.class.isAssignableFrom(returnType)));
    }

    private static UserAccount blockedAccount() {
        UserAccount account = new UserAccount(100.0);
        account.debit(125.0);
        return account;
    }
}
