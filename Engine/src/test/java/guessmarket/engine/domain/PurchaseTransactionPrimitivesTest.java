package guessmarket.engine.domain;

import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseTransactionPrimitivesTest {
    @Test
    void debitValidationAllowsOverdraftWithoutChangingAccount() {
        UserAccount account = new UserAccount(10.0);

        account.validateDebit(15.0);

        assertEquals(10.0, account.getBalance());
        assertEquals(UserStatus.ACTIVE, account.getStatus());
    }

    @Test
    void validatedDebitAppliesOverdraftAndBlockingWithoutFurtherValidation() {
        UserAccount account = new UserAccount(10.0);
        account.validateDebit(15.0);

        account.applyValidatedDebit(15.0);

        assertEquals(-5.0, account.getBalance());
        assertEquals(UserStatus.BLOCKED, account.getStatus());
    }

    @Test
    void creditValidationDetectsOverflowWithoutChangingAccount() {
        UserAccount account = new UserAccount(Double.MAX_VALUE);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> account.validateCredit(Double.MAX_VALUE));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(Double.MAX_VALUE, account.getBalance());
    }

    @Test
    void positionValidationDoesNotCreateOrChangePosition() {
        UserAccount account = new UserAccount(100.0);

        account.validateExecutedPurchase(7, 1, 3L, 12.5);

        assertFalse(account.hasPosition(7));
        assertEquals(0L, account.getSharesForOption(7, 1));
        assertEquals(0.0, account.getAmountPaidForOption(7, 1));
    }

    @Test
    void validatedPositionApplyCreatesExpectedHolding() {
        UserAccount account = new UserAccount(100.0);
        account.validateExecutedPurchase(7, 1, 3L, 12.5);

        account.applyValidatedExecutedPurchase(7, 1, 3L, 12.5);

        assertTrue(account.hasPosition(7));
        assertEquals(3L, account.getSharesForOption(7, 1));
        assertEquals(12.5, account.getAmountPaidForOption(7, 1));
    }

    @Test
    void optionValidationDetectsOverflowWithoutChangingShares() {
        MarketOption option = new MarketOption(1, "Yes");
        option.addShares(1L);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> option.validateAddShares(Long.MAX_VALUE));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(1L, option.getPurchasedShares());
    }

    @Test
    void eventAccountCanValidateAndApplyShareCostWithoutCommission() {
        EventAccount account = new EventAccount(10.0);

        account.validateShareCostCredit(7.5);
        assertEquals(10.0, account.getBalance());
        account.applyValidatedShareCostCredit(7.5);

        assertEquals(17.5, account.getBalance());
        assertEquals(0.0, account.getTotalCommissionCollected());
    }

    @Test
    void legacyTradeExposesBuyerAbsenceExplicitly() {
        Trade trade = new Trade(1, 1, "Yes", 2L, 5.0, 0.5, 5.5);

        assertEquals(Optional.empty(), trade.buyerName());
    }

    @Test
    void userAwareTradeStoresCanonicalBuyerName() {
        Trade trade = new Trade(1, 1, "Yes", 2L, 5.0, 0.5, 5.5, " Alice ");

        assertEquals(Optional.of("Alice"), trade.buyerName());
    }

    @Test
    void userAwareTradeRejectsMissingOrBlankBuyer() {
        assertThrows(
                NullPointerException.class,
                () -> new Trade(1, 1, "Yes", 2L, 5.0, 0.5, 5.5, (String) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Trade(1, 1, "Yes", 2L, 5.0, 0.5, 5.5, "   "));
    }
}
