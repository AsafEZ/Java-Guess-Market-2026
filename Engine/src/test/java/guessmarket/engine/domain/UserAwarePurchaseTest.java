package guessmarket.engine.domain;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingOperations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAwarePurchaseTest {
    @Test
    void differentBuyerCompletesAllPurchaseTransfersExactlyOnce() {
        PurchaseFixture fixture = createOpenedFixture(5, CommissionType.ON_PURCHASE, 500.0, 500.0);
        PurchaseQuote quote = fixture.event.quotePurchase(1, 3L);
        double buyerBefore = fixture.buyer.getBalance();
        double marketMakerBefore = fixture.marketMaker.getBalance();
        double eventBefore = fixture.event.getAccount().getBalance();

        PurchaseOutcome outcome = fixture.system.purchaseShares("Buyer", 1, 1, 3L);

        assertEquals(buyerBefore - quote.totalCharge(), fixture.buyer.getBalance(), 1.0e-12);
        assertEquals(
                marketMakerBefore + quote.commission(),
                fixture.marketMaker.getBalance(),
                1.0e-12);
        assertEquals(
                eventBefore + quote.shareCost(),
                fixture.event.getAccount().getBalance(),
                1.0e-12);
        assertEquals(3L, fixture.buyer.getSharesForOption(1, 1));
        assertEquals(quote.shareCost(), fixture.buyer.getAmountPaidForOption(1, 1));
        assertEquals(3L, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(1, fixture.event.getTrades().size());
        assertEquals(Optional.of("Buyer"), fixture.event.getTrades().getFirst().buyerName());
        assertOutcomeMatchesQuote(outcome, quote);
    }

    @Test
    void commissionIsNotStoredInEventAccountOrPosition() {
        PurchaseFixture fixture = createOpenedFixture(10, CommissionType.ON_PURCHASE, 500.0, 500.0);
        PurchaseQuote quote = fixture.event.quotePurchase(1, 4L);
        double eventBefore = fixture.event.getAccount().getBalance();

        fixture.system.purchaseShares("Buyer", 1, 1, 4L);

        assertEquals(
                eventBefore + quote.shareCost(),
                fixture.event.getAccount().getBalance(),
                1.0e-12);
        assertEquals(0.0, fixture.event.getAccount().getTotalCommissionCollected());
        assertEquals(quote.shareCost(), fixture.buyer.getTotalAmountPaid(1), 1.0e-12);
    }

    @Test
    void marketMakerBuyingOwnEventPaysNetShareCost() {
        PurchaseFixture fixture = createOpenedFixture(10, CommissionType.ON_PURCHASE, 500.0, 500.0);
        PurchaseQuote quote = fixture.event.quotePurchase(1, 4L);
        double marketMakerBefore = fixture.marketMaker.getBalance();

        PurchaseOutcome outcome = fixture.system.purchaseShares("Maker", 1, 1, 4L);

        assertEquals(
                marketMakerBefore - quote.shareCost(),
                fixture.marketMaker.getBalance(),
                1.0e-12);
        assertEquals(4L, fixture.marketMaker.getSharesForOption(1, 1));
        assertEquals(quote.shareCost(), fixture.marketMaker.getAmountPaidForOption(1, 1));
        assertOutcomeMatchesQuote(outcome, quote);
    }

    @Test
    void activeBuyerMayOverdrawAndBecomesBlockedAfterCompletePurchase() {
        PurchaseFixture fixture = createOpenedFixture(5, CommissionType.ON_PURCHASE, 1.0, 500.0);

        fixture.system.purchaseShares("Buyer", 1, 1, 20L);

        assertTrue(fixture.buyer.getBalance() < 0.0);
        assertEquals(UserStatus.BLOCKED, fixture.buyer.getStatus());
        assertEquals(20L, fixture.buyer.getSharesForOption(1, 1));
        assertEquals(20L, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(1, fixture.event.getTrades().size());
    }

    @Test
    void blockedBuyerCannotMakeAnotherPurchase() {
        PurchaseFixture fixture = createOpenedFixture(5, CommissionType.ON_PURCHASE, 1.0, 500.0);
        fixture.system.purchaseShares("Buyer", 1, 1, 20L);
        PurchaseState state = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.purchaseShares("Buyer", 1, 2, 1L));

        assertEquals(ErrorCode.USER_ACCOUNT_BLOCKED, exception.getErrorCode());
        assertStateEquals(state, fixture);
    }

    @Test
    void passiveCommissionCreditIsAllowedForBlockedMarketMaker() {
        PurchaseFixture fixture = createOpenedFixture(10, CommissionType.ON_PURCHASE, 500.0, 500.0);
        fixture.marketMaker.debit(fixture.marketMaker.getBalance() + 1.0);
        double blockedBalance = fixture.marketMaker.getBalance();
        PurchaseQuote quote = fixture.event.quotePurchase(1, 3L);

        fixture.system.purchaseShares("Buyer", 1, 1, 3L);

        assertEquals(UserStatus.BLOCKED, fixture.marketMaker.getStatus());
        assertEquals(
                blockedBalance + quote.commission(),
                fixture.marketMaker.getBalance(),
                1.0e-12);
    }

    @Test
    void notStartedAndClosedEventsRejectPurchaseWithoutMutation() {
        MarketSystem notStartedSystem = new MarketSystem();
        MarketEvent notStarted = createNotStartedEvent(1, 5, CommissionType.ON_PURCHASE);
        User buyer = new User("Buyer", 100.0);
        User maker = new User("Maker", 100.0);
        notStartedSystem.addUser(buyer);
        notStartedSystem.addUser(maker);
        notStartedSystem.addEvent(notStarted);
        notStartedSystem.assignMarketMaker(1, "Maker");

        EngineException notStartedFailure = assertThrows(
                EngineException.class,
                () -> notStartedSystem.purchaseShares("Buyer", 1, 1, 1L));
        assertEquals(ErrorCode.EVENT_NOT_STARTED, notStartedFailure.getErrorCode());
        assertFalse(buyer.hasPosition(1));

        PurchaseFixture closedFixture = createOpenedFixture(
                5,
                CommissionType.ON_PURCHASE,
                100.0,
                500.0);
        closedFixture.event.close(1);
        PurchaseState closedState = captureState(closedFixture);

        EngineException closedFailure = assertThrows(
                EngineException.class,
                () -> closedFixture.system.purchaseShares("Buyer", 1, 1, 1L));
        assertEquals(ErrorCode.EVENT_ALREADY_CLOSED, closedFailure.getErrorCode());
        assertStateEquals(closedState, closedFixture);
    }

    @Test
    void eventWithoutMarketMakerRejectsPurchaseWithoutPosition() {
        MarketSystem system = new MarketSystem();
        MarketEvent event = createLegacyActiveEvent(1, 5, CommissionType.ON_PURCHASE, 20.0);
        User buyer = new User("Buyer", 100.0);
        system.addUser(buyer);
        system.addEvent(event);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.purchaseShares("Buyer", 1, 1, 1L));

        assertEquals(ErrorCode.MARKET_MAKER_NOT_ASSIGNED, exception.getErrorCode());
        assertFalse(buyer.hasPosition(1));
        assertEquals(0L, event.findOption(1).getPurchasedShares());
    }

    @Test
    void missingUserAndEventUseExistingLookupErrors() {
        MarketSystem system = new MarketSystem();

        EngineException missingUser = assertThrows(
                EngineException.class,
                () -> system.purchaseShares("Missing", 99, 1, 1L));
        assertEquals(ErrorCode.USER_NOT_FOUND, missingUser.getErrorCode());

        system.addUser(new User("Buyer", 100.0));
        EngineException missingEvent = assertThrows(
                EngineException.class,
                () -> system.purchaseShares("Buyer", 99, 1, 1L));
        assertEquals(ErrorCode.EVENT_NOT_FOUND, missingEvent.getErrorCode());
    }

    @Test
    void invalidOptionAndQuantityRejectPurchaseWithoutMutation() {
        PurchaseFixture fixture = createOpenedFixture(5, CommissionType.ON_PURCHASE, 100.0, 500.0);
        PurchaseState state = captureState(fixture);

        EngineException invalidOption = assertThrows(
                EngineException.class,
                () -> fixture.system.purchaseShares("Buyer", 1, 3, 1L));
        assertEquals(ErrorCode.INVALID_OPTION_NUMBER, invalidOption.getErrorCode());
        assertStateEquals(state, fixture);

        EngineException invalidQuantity = assertThrows(
                EngineException.class,
                () -> fixture.system.purchaseShares("Buyer", 1, 1, 0L));
        assertEquals(ErrorCode.INVALID_SHARE_QUANTITY, invalidQuantity.getErrorCode());
        assertStateEquals(state, fixture);
    }

    @Test
    void marketMakerCreditOverflowLeavesTransactionUnchanged() {
        PurchaseFixture fixture = createOpenedFixture(
                10,
                CommissionType.ON_PURCHASE,
                100.0,
                Double.MAX_VALUE,
                new FixedHighCostMechanism());
        PurchaseQuote quote = fixture.event.quotePurchase(1, 3L);
        assertTrue(Double.isFinite(quote.shareCost()));
        assertTrue(Double.isFinite(quote.commission()));
        assertTrue(Double.isFinite(quote.totalCharge()));
        PurchaseState state = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.purchaseShares("Buyer", 1, 1, 3L));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertStateEquals(state, fixture);
        assertFalse(fixture.buyer.hasPosition(1));
    }

    @Test
    void eventAccountOverflowLeavesTransactionUnchanged() {
        MarketSystem system = new MarketSystem();
        MarketEvent event = createLegacyActiveEvent(
                1,
                0,
                CommissionType.ON_PURCHASE,
                Double.MAX_VALUE,
                new FixedHighCostMechanism());
        User buyer = new User("Buyer", 100.0);
        User maker = new User("Maker", 100.0);
        system.addUser(buyer);
        system.addUser(maker);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        PurchaseFixture fixture = new PurchaseFixture(system, event, buyer, maker);
        PurchaseQuote quote = event.quotePurchase(1, 3L);
        assertTrue(Double.isFinite(quote.shareCost()));
        assertTrue(Double.isFinite(quote.commission()));
        assertTrue(Double.isFinite(quote.totalCharge()));
        PurchaseState state = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.purchaseShares("Buyer", 1, 1, 3L));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertStateEquals(state, fixture);
    }

    @Test
    void positionOverflowLeavesTransactionUnchanged() {
        PurchaseFixture fixture = createOpenedFixture(0, CommissionType.ON_PURCHASE, 500.0, 500.0);
        fixture.buyer.recordExecutedPurchase(1, 1, Long.MAX_VALUE, 1.0);
        PurchaseState state = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.purchaseShares("Buyer", 1, 1, 1L));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertStateEquals(state, fixture);
    }

    @Test
    void legacyPurchaseStillCreditsCommissionToEventAndHasNoBuyer() {
        MarketEvent event = createLegacyActiveEvent(1, 5, CommissionType.ON_PURCHASE, 20.0);
        PurchaseQuote quote = event.quotePurchase(1, 3L);

        PurchaseOutcome outcome = event.purchase(1, 3L);

        assertEquals(20.0 + quote.totalCharge(), event.getAccount().getBalance(), 1.0e-12);
        assertEquals(quote.commission(), event.getAccount().getTotalCommissionCollected());
        assertEquals(Optional.empty(), event.getTrades().getFirst().buyerName());
        assertOutcomeMatchesQuote(outcome, quote);
    }

    private static PurchaseFixture createOpenedFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double buyerBalance,
            double marketMakerBalance) {
        return createOpenedFixture(
                commissionPercentage,
                commissionType,
                buyerBalance,
                marketMakerBalance,
                createMechanism());
    }

    private static PurchaseFixture createOpenedFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double buyerBalance,
            double marketMakerBalance,
            LmsrTradingOperations mechanism) {
        MarketSystem system = new MarketSystem();
        User buyer = new User("Buyer", buyerBalance);
        User marketMaker = new User("Maker", marketMakerBalance);
        MarketEvent event = createNotStartedEvent(
                1,
                commissionPercentage,
                commissionType,
                mechanism);
        system.addUser(buyer);
        system.addUser(marketMaker);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        system.openEvent(1, "Maker");
        return new PurchaseFixture(system, event, buyer, marketMaker);
    }

    private static MarketEvent createNotStartedEvent(
            int eventId,
            int commissionPercentage,
            CommissionType commissionType) {
        return createNotStartedEvent(
                eventId,
                commissionPercentage,
                commissionType,
                createMechanism());
    }

    private static MarketEvent createNotStartedEvent(
            int eventId,
            int commissionPercentage,
            CommissionType commissionType,
            LmsrTradingOperations mechanism) {
        return MarketEvent.createNotStartedEvent(
                eventId,
                "Event " + eventId,
                "Description",
                createOptions(),
                new CommissionPolicy(commissionPercentage, commissionType),
                mechanism);
    }

    private static MarketEvent createLegacyActiveEvent(
            int eventId,
            int commissionPercentage,
            CommissionType commissionType,
            double initialBalance) {
        return createLegacyActiveEvent(
                eventId,
                commissionPercentage,
                commissionType,
                initialBalance,
                createMechanism());
    }

    private static MarketEvent createLegacyActiveEvent(
            int eventId,
            int commissionPercentage,
            CommissionType commissionType,
            double initialBalance,
            LmsrTradingOperations mechanism) {
        return new MarketEvent(
                eventId,
                "Event " + eventId,
                "Description",
                createOptions(),
                new CommissionPolicy(commissionPercentage, commissionType),
                mechanism,
                initialBalance);
    }

    private static List<MarketOption> createOptions() {
        return List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No"));
    }

    private static LmsrTradingMechanism createMechanism() {
        return new LmsrTradingMechanism(10, new LmsrCalculator());
    }

    private static PurchaseState captureState(PurchaseFixture fixture) {
        return new PurchaseState(
                fixture.buyer.getBalance(),
                fixture.buyer.getStatus(),
                fixture.marketMaker.getBalance(),
                fixture.marketMaker.getStatus(),
                fixture.event.getAccount().getBalance(),
                fixture.event.getAccount().getTotalCommissionCollected(),
                fixture.event.getStatus(),
                fixture.event.findOption(1).getPurchasedShares(),
                fixture.event.findOption(2).getPurchasedShares(),
                fixture.event.getTrades().size(),
                fixture.buyer.hasPosition(1),
                fixture.buyer.getSharesForOption(1, 1),
                fixture.buyer.getAmountPaidForOption(1, 1));
    }

    private static void assertStateEquals(PurchaseState expected, PurchaseFixture actual) {
        assertEquals(expected.buyerBalance, actual.buyer.getBalance());
        assertEquals(expected.buyerStatus, actual.buyer.getStatus());
        assertEquals(expected.marketMakerBalance, actual.marketMaker.getBalance());
        assertEquals(expected.marketMakerStatus, actual.marketMaker.getStatus());
        assertEquals(expected.eventBalance, actual.event.getAccount().getBalance());
        assertEquals(
                expected.eventCommission,
                actual.event.getAccount().getTotalCommissionCollected());
        assertEquals(expected.eventStatus, actual.event.getStatus());
        assertEquals(expected.firstOptionShares, actual.event.findOption(1).getPurchasedShares());
        assertEquals(expected.secondOptionShares, actual.event.findOption(2).getPurchasedShares());
        assertEquals(expected.tradeCount, actual.event.getTrades().size());
        assertEquals(expected.hasPosition, actual.buyer.hasPosition(1));
        assertEquals(expected.positionShares, actual.buyer.getSharesForOption(1, 1));
        assertEquals(expected.positionPaid, actual.buyer.getAmountPaidForOption(1, 1));
    }

    private static void assertOutcomeMatchesQuote(PurchaseOutcome outcome, PurchaseQuote quote) {
        assertEquals(quote.optionNumber(), outcome.optionNumber());
        assertEquals(quote.quantity(), outcome.shareQuantity());
        assertEquals(quote.shareCost(), outcome.shareCost());
        assertEquals(quote.commission(), outcome.commission());
        assertEquals(quote.totalCharge(), outcome.totalPaid());
    }

    private record PurchaseFixture(
            MarketSystem system,
            MarketEvent event,
            User buyer,
            User marketMaker) {
    }

    private record PurchaseState(
            double buyerBalance,
            UserStatus buyerStatus,
            double marketMakerBalance,
            UserStatus marketMakerStatus,
            double eventBalance,
            double eventCommission,
            EventStatus eventStatus,
            long firstOptionShares,
            long secondOptionShares,
            int tradeCount,
            boolean hasPosition,
            long positionShares,
            double positionPaid) {
    }

    private static final class FixedHighCostMechanism implements LmsrTradingOperations {
        @Override
        public double calculatePurchaseCost(
                List<MarketOption> options,
                int optionNumber,
                long quantity) {
            return Double.MAX_VALUE / 20.0;
        }

        @Override
        public double calculateOptionValue(List<MarketOption> options, int optionNumber) {
            return 0.5;
        }

        @Override
        public double calculateInitialSubsidy() {
            return 1.0;
        }

        @Override
        public int getB() {
            return 1;
        }

        @Override
        public TradingMethod getTradingMethod() {
            return TradingMethod.LMSR;
        }
    }
}
