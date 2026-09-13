package guessmarket.engine.domain;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketPositionTest {
    @Test
    void createsEmptyPositionForValidEvent() {
        MarketPosition position = new MarketPosition(7);

        assertEquals(7, position.getEventId());
        assertTrue(position.getOptionNumbers().isEmpty());
    }

    @Test
    void unknownOptionStartsWithZeroSharesAndAmount() {
        MarketPosition position = new MarketPosition(7);

        assertEquals(0L, position.getSharesForOption(1));
        assertEquals(0.0, position.getAmountPaidForOption(1));
        assertEquals(0.0, position.getCommissionPaidForOption(1));
        assertEquals(0L, position.getTotalShares());
        assertEquals(0.0, position.getTotalAmountPaid());
        assertEquals(0.0, position.getTotalCommissionPaid());
    }

    @Test
    void recordsPurchaseForOneOption() {
        MarketPosition position = new MarketPosition(7);

        position.recordPurchase(1, 4L, 12.5, 1.25);

        assertEquals(4L, position.getSharesForOption(1));
        assertEquals(12.5, position.getAmountPaidForOption(1));
        assertEquals(1.25, position.getCommissionPaidForOption(1));
        assertEquals(Set.of(1), position.getOptionNumbers());
    }

    @Test
    void accumulatesPurchasesForSameOption() {
        MarketPosition position = new MarketPosition(7);

        position.recordPurchase(1, 4L, 12.5, 1.25);
        position.recordPurchase(1, 3L, 8.25, 0.75);

        assertEquals(7L, position.getSharesForOption(1));
        assertEquals(20.75, position.getAmountPaidForOption(1));
        assertEquals(2.0, position.getCommissionPaidForOption(1));
    }

    @Test
    void keepsDifferentOptionsSeparate() {
        MarketPosition position = new MarketPosition(7);

        position.recordPurchase(1, 4L, 12.5, 1.25);
        position.recordPurchase(2, 6L, 21.0, 2.1);

        assertEquals(4L, position.getSharesForOption(1));
        assertEquals(12.5, position.getAmountPaidForOption(1));
        assertEquals(1.25, position.getCommissionPaidForOption(1));
        assertEquals(6L, position.getSharesForOption(2));
        assertEquals(21.0, position.getAmountPaidForOption(2));
        assertEquals(2.1, position.getCommissionPaidForOption(2));
    }

    @Test
    void calculatesTotalShares() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 4L, 12.5);
        position.recordPurchase(2, 6L, 21.0);

        assertEquals(10L, position.getTotalShares());
    }

    @Test
    void calculatesTotalAmountPaid() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 4L, 12.5);
        position.recordPurchase(2, 6L, 21.0);

        assertEquals(33.5, position.getTotalAmountPaid());
    }

    @Test
    void calculatesTotalCommissionPaidAcrossOptions() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 4L, 12.5, 1.25);
        position.recordPurchase(2, 6L, 21.0, 2.1);

        assertEquals(3.35, position.getTotalCommissionPaid(), 1.0e-12);
    }

    @Test
    void legacyPurchaseOverloadRecordsZeroCommission() {
        MarketPosition position = new MarketPosition(7);

        position.recordPurchase(1, 4L, 12.5);

        assertEquals(0.0, position.getCommissionPaidForOption(1));
        assertEquals(0.0, position.getTotalCommissionPaid());
    }

    @Test
    void acceptsZeroAndNegativeEventIds() {
        assertEquals(0, new MarketPosition(0).getEventId());
        assertEquals(-1, new MarketPosition(-1).getEventId());
    }

    @Test
    void rejectsInvalidOptionNumberWithoutChangingState() {
        MarketPosition position = positionWithPurchase();

        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(0, 2L, 5.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(-1, 2L, 5.0));

        assertUnchangedInitialPurchase(position);
    }

    @Test
    void rejectsNonPositiveQuantityWithoutChangingState() {
        MarketPosition position = positionWithPurchase();

        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 0L, 5.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, -1L, 5.0));

        assertUnchangedInitialPurchase(position);
    }

    @Test
    void rejectsInvalidPaidAmountWithoutChangingState() {
        MarketPosition position = positionWithPurchase();

        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, 0.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, -1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, Double.POSITIVE_INFINITY));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, Double.NEGATIVE_INFINITY));

        assertUnchangedInitialPurchase(position);
    }

    @Test
    void rejectsInvalidCommissionWithoutChangingState() {
        MarketPosition position = positionWithPurchase();

        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, 5.0, -1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, 5.0, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, 5.0, Double.POSITIVE_INFINITY));
        assertThrows(
                IllegalArgumentException.class,
                () -> position.recordPurchase(1, 2L, 5.0, Double.NEGATIVE_INFINITY));

        assertUnchangedInitialPurchase(position);
    }

    @Test
    void preventsShareOverflowWithoutChangingState() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, Long.MAX_VALUE, 1.0);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> position.recordPurchase(1, 1L, 1.0));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(Long.MAX_VALUE, position.getSharesForOption(1));
        assertEquals(1.0, position.getAmountPaidForOption(1));
    }

    @Test
    void preventsAmountOverflowWithoutChangingState() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 1L, Double.MAX_VALUE);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> position.recordPurchase(1, 1L, Double.MAX_VALUE));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(1L, position.getSharesForOption(1));
        assertEquals(Double.MAX_VALUE, position.getAmountPaidForOption(1));
    }

    @Test
    void preventsCommissionOverflowWithoutChangingState() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 1L, 2.0, Double.MAX_VALUE);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> position.recordPurchase(1, 2L, 3.0, Double.MAX_VALUE));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(1L, position.getSharesForOption(1));
        assertEquals(2.0, position.getAmountPaidForOption(1));
        assertEquals(Double.MAX_VALUE, position.getCommissionPaidForOption(1));
    }

    @Test
    void additionalCommissionChangesOnlyCommissionBookkeeping() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 4L, 12.5, 1.25);

        position.validateAdditionalCommission(1, 0.75);
        position.applyValidatedAdditionalCommission(1, 0.75);

        assertEquals(4L, position.getSharesForOption(1));
        assertEquals(12.5, position.getAmountPaidForOption(1));
        assertEquals(2.0, position.getCommissionPaidForOption(1));
    }

    @Test
    void optionNumbersAreAnImmutableSnapshot() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 1L, 2.0);
        Set<Integer> optionNumbers = position.getOptionNumbers();

        assertThrows(UnsupportedOperationException.class, () -> optionNumbers.add(2));

        position.recordPurchase(2, 1L, 3.0);
        assertEquals(Set.of(1), optionNumbers);
        assertEquals(Set.of(1, 2), position.getOptionNumbers());
    }

    private static MarketPosition positionWithPurchase() {
        MarketPosition position = new MarketPosition(7);
        position.recordPurchase(1, 4L, 12.5);
        return position;
    }

    private static void assertUnchangedInitialPurchase(MarketPosition position) {
        assertEquals(4L, position.getSharesForOption(1));
        assertEquals(12.5, position.getAmountPaidForOption(1));
        assertEquals(0.0, position.getCommissionPaidForOption(1));
        assertEquals(4L, position.getTotalShares());
        assertEquals(12.5, position.getTotalAmountPaid());
        assertEquals(0.0, position.getTotalCommissionPaid());
        assertEquals(Set.of(1), position.getOptionNumbers());
    }
}
