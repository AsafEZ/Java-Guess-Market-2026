package guessmarket.engine.domain;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingOperations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseQuoteTest {
    @Test
    void quoteReturnsExpectedLmsrShareCost() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        PurchaseQuote quote = event.quotePurchase(1, 3L);

        double expected = new LmsrCalculator().purchaseCost(10, 0, 0, 0, 3L);
        assertEquals(expected, quote.shareCost(), 1.0e-12);
    }

    @Test
    void quoteCalculatesPurchaseCommission() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        PurchaseQuote quote = event.quotePurchase(1, 3L);

        assertEquals(quote.shareCost() * 0.05, quote.commission(), 1.0e-12);
    }

    @Test
    void totalChargeIsShareCostPlusCommission() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        PurchaseQuote quote = event.quotePurchase(1, 3L);

        assertEquals(quote.shareCost() + quote.commission(), quote.totalCharge());
    }

    @Test
    void quoteDoesNotChangePurchasedShares() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        event.quotePurchase(1, 3L);

        assertEquals(0L, event.findOption(1).getPurchasedShares());
        assertEquals(0L, event.findOption(2).getPurchasedShares());
    }

    @Test
    void quoteDoesNotChangeEventAccount() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);
        double initialBalance = event.getAccount().getBalance();

        event.quotePurchase(1, 3L);

        assertEquals(initialBalance, event.getAccount().getBalance());
        assertEquals(0.0, event.getAccount().getTotalCommissionCollected());
    }

    @Test
    void quoteDoesNotAddTrade() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        event.quotePurchase(1, 3L);

        assertEquals(List.of(), event.getTrades());
    }

    @Test
    void repeatedQuotesReturnEqualValues() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);

        PurchaseQuote first = event.quotePurchase(1, 3L);
        PurchaseQuote second = event.quotePurchase(1, 3L);

        assertEquals(first, second);
    }

    @Test
    void notStartedEventRejectsQuoteWithoutMutation() {
        MarketEvent event = createNotStartedEvent();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(1, 3L));

        assertEquals(ErrorCode.EVENT_NOT_STARTED, exception.getErrorCode());
        assertUnchanged(event, EventStatus.NOT_STARTED, 0.0);
    }

    @Test
    void closedEventRejectsQuoteWithoutMutation() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);
        event.close(1);
        double closedBalance = event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(1, 3L));

        assertEquals(ErrorCode.EVENT_ALREADY_CLOSED, exception.getErrorCode());
        assertUnchanged(event, EventStatus.CLOSED, closedBalance);
    }

    @Test
    void invalidOptionNumberIsRejectedWithoutMutation() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);
        double initialBalance = event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(3, 1L));

        assertEquals(ErrorCode.INVALID_OPTION_NUMBER, exception.getErrorCode());
        assertUnchanged(event, EventStatus.ACTIVE, initialBalance);
    }

    @Test
    void invalidQuantityIsRejectedWithoutMutation() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);
        double initialBalance = event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(1, 0L));

        assertEquals(ErrorCode.INVALID_SHARE_QUANTITY, exception.getErrorCode());
        assertUnchanged(event, EventStatus.ACTIVE, initialBalance);
    }

    @Test
    void arithmeticOverflowIsRejectedWithoutAdditionalMutation() {
        MarketEvent event = createActiveEvent(0, CommissionType.ON_PURCHASE);
        event.purchase(1, 1L);
        double existingBalance = event.getAccount().getBalance();
        int existingTrades = event.getTrades().size();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(1, Long.MAX_VALUE));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(1L, event.findOption(1).getPurchasedShares());
        assertEquals(existingBalance, event.getAccount().getBalance());
        assertEquals(existingTrades, event.getTrades().size());
    }

    @Test
    void nonFiniteCostIsRejectedWithoutMutation() {
        MarketEvent event = createActiveEvent(
                5,
                CommissionType.ON_PURCHASE,
                new NonFiniteCostMechanism());
        double initialBalance = event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.quotePurchase(1, 1L));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertUnchanged(event, EventStatus.ACTIVE, initialBalance);
    }

    @Test
    void legacyPurchaseAfterQuoteMutatesOnceAndMatchesQuote() {
        MarketEvent event = createActiveEvent(5, CommissionType.ON_PURCHASE);
        double initialBalance = event.getAccount().getBalance();
        PurchaseQuote quote = event.quotePurchase(1, 3L);

        PurchaseOutcome outcome = event.purchase(1, 3L);

        assertEquals(3L, event.findOption(1).getPurchasedShares());
        assertEquals(initialBalance + quote.totalCharge(), event.getAccount().getBalance());
        assertEquals(1, event.getTrades().size());
        assertEquals(quote.optionNumber(), outcome.optionNumber());
        assertEquals(quote.quantity(), outcome.shareQuantity());
        assertEquals(quote.shareCost(), outcome.shareCost());
        assertEquals(quote.commission(), outcome.commission());
        assertEquals(quote.totalCharge(), outcome.totalPaid());
    }

    private static MarketEvent createActiveEvent(
            int commissionPercentage,
            CommissionType commissionType) {
        return createActiveEvent(commissionPercentage, commissionType, createMechanism());
    }

    private static MarketEvent createActiveEvent(
            int commissionPercentage,
            CommissionType commissionType,
            LmsrTradingOperations mechanism) {
        return new MarketEvent(
                1,
                "Event",
                "Description",
                createOptions(),
                new CommissionPolicy(commissionPercentage, commissionType),
                mechanism,
                20.0);
    }

    private static MarketEvent createNotStartedEvent() {
        return MarketEvent.createNotStartedEvent(
                1,
                "Event",
                "Description",
                createOptions(),
                new CommissionPolicy(5, CommissionType.ON_PURCHASE),
                createMechanism());
    }

    private static List<MarketOption> createOptions() {
        return List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No"));
    }

    private static LmsrTradingMechanism createMechanism() {
        return new LmsrTradingMechanism(10, new LmsrCalculator());
    }

    private static void assertUnchanged(
            MarketEvent event,
            EventStatus expectedStatus,
            double expectedBalance) {
        assertEquals(expectedStatus, event.getStatus());
        assertEquals(0L, event.findOption(1).getPurchasedShares());
        assertEquals(0L, event.findOption(2).getPurchasedShares());
        assertEquals(expectedBalance, event.getAccount().getBalance());
        assertEquals(List.of(), event.getTrades());
    }

    private static final class NonFiniteCostMechanism implements LmsrTradingOperations {
        @Override
        public double calculatePurchaseCost(
                List<MarketOption> options,
                int optionNumber,
                long quantity) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double calculateOptionValue(List<MarketOption> options, int optionNumber) {
            return 0.5;
        }

        @Override
        public double calculateInitialSubsidy() {
            return 10.0;
        }

        @Override
        public int getB() {
            return 10;
        }

        @Override
        public TradingMethod getTradingMethod() {
            return TradingMethod.LMSR;
        }
    }
}
