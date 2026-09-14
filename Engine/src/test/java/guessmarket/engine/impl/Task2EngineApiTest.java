package guessmarket.engine.impl;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CommissionPolicy;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.User;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.LmsrEventDetails;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingOperations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Task2EngineApiTest {

    @Test
    void getAllMarketEventsPreservesDomainInsertionOrder() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        MarketEvent secondEvent = createNotStartedEvent(
                2, CommissionType.ON_CLOSE, new LmsrTradingMechanism(20, new LmsrCalculator()));
        fixture.system.addEvent(secondEvent);

        List<MarketEventSummary> events = fixture.engine.getAllMarketEvents();

        assertEquals(List.of(1, 2), events.stream()
                .map(MarketEventSummary::eventId)
                .toList());
        assertEquals(List.of("Event 1", "Event 2"), events.stream()
                .map(MarketEventSummary::name)
                .toList());
    }

    @Test
    void getMarketEventDetailsReturnsLmsrProjection() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);

        MarketEventDetails details = fixture.engine.getMarketEventDetails(1);

        assertEquals(1, details.eventId());
        assertEquals(EventStatus.NOT_STARTED, details.status());
        assertEquals(TradingMethod.LMSR, details.tradingMethod());
        assertEquals("Maker", details.marketMakerName().orElseThrow());
        assertEquals(10, assertInstanceOf(
                LmsrEventDetails.class, details.mechanismDetails()).b());
    }

    @Test
    void getAllUsersPreservesRegistryOrder() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);

        List<UserSummary> users = fixture.engine.getAllUsers();

        assertEquals(List.of("Maker", "Buyer", "Observer"), users.stream()
                .map(UserSummary::name)
                .toList());
    }

    @Test
    void getUserDetailsMapsCurrentBalanceStatusPositionsAndMakerEvents() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        fixture.engine.openEvent(1, "Maker");
        fixture.engine.purchaseShares(1, "Buyer", 1, 3L);

        UserDetails buyer = fixture.engine.getUserDetails(" Buyer ");
        UserDetails maker = fixture.engine.getUserDetails("Maker");

        assertEquals(fixture.buyer.getBalance(), buyer.balance());
        assertEquals(UserStatus.ACTIVE, buyer.status());
        assertEquals(1, buyer.positions().size());
        assertEquals(3L, buyer.positions().getFirst().totalShares());
        assertEquals(1, buyer.positions().getFirst().tradesNewestFirst().size());
        assertEquals(List.of(), buyer.marketMakerEventIds().stream().toList());
        assertEquals(Set.of(1), maker.marketMakerEventIds());
    }

    @Test
    void marketMakerCanOpenEventAndReceivesUpdatedEventSnapshot() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        double makerBefore = fixture.marketMaker.getBalance();

        MarketEventDetails details = fixture.engine.openEvent(1, " Maker ");

        assertEquals(EventStatus.ACTIVE, details.status());
        assertTrue(details.accountBalance() > 0.0);
        assertTrue(fixture.marketMaker.getBalance() < makerBefore);
    }

    @Test
    void nonMarketMakerCannotOpenAndEngineDoesNotMutateAfterFailure() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        double buyerBefore = fixture.buyer.getBalance();
        double makerBefore = fixture.marketMaker.getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.engine.openEvent(1, "Buyer"));

        assertEquals(ErrorCode.USER_NOT_MARKET_MAKER, exception.getErrorCode());
        assertEquals(EventStatus.NOT_STARTED, fixture.event.getStatus());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
        assertEquals(buyerBefore, fixture.buyer.getBalance());
        assertEquals(makerBefore, fixture.marketMaker.getBalance());
    }

    @Test
    void userAwarePurchaseReturnsUpdatedEventBalanceAndPosition() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        fixture.engine.openEvent(1, "Maker");

        UserPurchaseResult result = fixture.engine.purchaseShares(
                1, " Buyer ", 2, 4L);

        assertEquals("Buyer", result.buyerName());
        assertEquals(4L, result.shareQuantity());
        assertEquals(result.shareCost() + result.commission(), result.totalPaid());
        assertEquals(fixture.buyer.getBalance(), result.updatedBuyer().balance());
        assertEquals(4L, result.updatedBuyer().positions().getFirst().totalShares());
        assertEquals(result.shareCost(), result.updatedBuyer().positions()
                .getFirst().totalAmountPaid());
        assertEquals(result.commission(), result.updatedBuyer().positions()
                .getFirst().totalCommissionPaid());
        assertEquals(fixture.event.getAccount().getBalance(),
                result.updatedEvent().accountBalance());
    }

    @Test
    void purchaseBeyondBalanceCompletesAndReturnsBlockedBuyer() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 0.1);
        fixture.engine.openEvent(1, "Maker");

        UserPurchaseResult result = fixture.engine.purchaseShares(
                1, "Buyer", 1, 20L);

        assertTrue(result.updatedBuyer().balance() < 0.0);
        assertEquals(UserStatus.BLOCKED, result.updatedBuyer().status());
        assertEquals(20L, result.updatedBuyer().positions().getFirst().totalShares());
        assertEquals(20L, fixture.event.findOption(1).getPurchasedShares());
    }

    @Test
    void marketMakerCanCloseThroughAtomicSettlementPath() {
        Fixture fixture = createFixture(CommissionType.ON_CLOSE, 1_000.0);
        fixture.engine.openEvent(1, "Maker");
        fixture.engine.purchaseShares(1, "Buyer", 1, 3L);

        SettlementResult result = fixture.engine.closeEvent(1, " Maker ", 1);

        assertEquals(EventStatus.CLOSED, result.closedEvent().status());
        assertEquals(1, result.winningOptionNumber());
        assertEquals("Yes", result.winningOptionName());
        assertEquals(0.0, result.eventBalanceAfter());
        assertEquals(1, result.userSettlements().size());
        assertEquals("Buyer", result.userSettlements().getFirst().userName());
    }

    @Test
    void nonMarketMakerCannotCloseAndEngineDoesNotMutateAfterFailure() {
        Fixture fixture = createFixture(CommissionType.ON_CLOSE, 1_000.0);
        fixture.engine.openEvent(1, "Maker");
        fixture.engine.purchaseShares(1, "Buyer", 1, 3L);
        double eventBalance = fixture.event.getAccount().getBalance();
        double buyerBalance = fixture.buyer.getBalance();
        int tradeCount = fixture.event.getTrades().size();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.engine.closeEvent(1, "Buyer", 1));

        assertEquals(ErrorCode.USER_NOT_MARKET_MAKER, exception.getErrorCode());
        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
        assertNull(fixture.event.getWinningOptionNumber());
        assertEquals(eventBalance, fixture.event.getAccount().getBalance());
        assertEquals(buyerBalance, fixture.buyer.getBalance());
        assertEquals(tradeCount, fixture.event.getTrades().size());
    }

    @Test
    void marketMakerWinnerHasOneConsolidatedPublicCredit() {
        Fixture fixture = createFixture(CommissionType.ON_CLOSE, 1_000.0);
        fixture.engine.openEvent(1, "Maker");
        fixture.engine.purchaseShares(1, "Maker", 1, 2L);
        fixture.engine.purchaseShares(1, "Buyer", 1, 3L);

        SettlementResult result = fixture.engine.closeEvent(1, "Maker", 1);

        assertEquals(1L, result.accountCredits().stream()
                .filter(credit -> credit.userName().equals("Maker"))
                .count());
        assertEquals(2, result.userSettlements().size());
    }

    @Test
    void blockedWinnerReceivesPassiveCreditAndRemainsBlocked() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 0.1);
        fixture.engine.openEvent(1, "Maker");
        fixture.engine.purchaseShares(1, "Buyer", 1, 20L);
        double blockedBalance = fixture.buyer.getBalance();
        assertEquals(UserStatus.BLOCKED, fixture.buyer.getStatus());

        SettlementResult result = fixture.engine.closeEvent(1, "Maker", 1);

        assertTrue(fixture.buyer.getBalance() > blockedBalance);
        assertEquals(UserStatus.BLOCKED, fixture.buyer.getStatus());
        assertEquals(UserStatus.BLOCKED,
                fixture.engine.getUserDetails("Buyer").status());
        assertEquals("Buyer", result.userSettlements().getFirst().userName());
    }

    @Test
    void invalidUserNamesAreRejectedAtThePublicBoundary() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);
        List<String> invalidNames = Arrays.asList(null, "", "   ", "\t");

        for (String invalidName : invalidNames) {
            assertInvalidUserName(() -> fixture.engine.getUserDetails(invalidName));
            assertInvalidUserName(() -> fixture.engine.openEvent(1, invalidName));
            assertInvalidUserName(
                    () -> fixture.engine.purchaseShares(1, invalidName, 1, 1L));
            assertInvalidUserName(() -> fixture.engine.closeEvent(1, invalidName, 1));
        }
    }

    @Test
    void everyTask2OperationRejectsCallsBeforeSystemLoad() {
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        assertNoSystemLoaded(engine::getAllMarketEvents);
        assertNoSystemLoaded(() -> engine.getMarketEventDetails(1));
        assertNoSystemLoaded(engine::getAllUsers);
        assertNoSystemLoaded(() -> engine.getUserDetails(null));
        assertNoSystemLoaded(() -> engine.openEvent(1, null));
        assertNoSystemLoaded(() -> engine.purchaseShares(1, null, 1, 1L));
        assertNoSystemLoaded(() -> engine.closeEvent(1, null, 1));
    }

    @Test
    void domainEngineExceptionsAndCodesPropagateUnchanged() {
        Fixture fixture = createFixture(CommissionType.ON_PURCHASE, 1_000.0);

        EngineException missingUser = assertThrows(
                EngineException.class,
                () -> fixture.engine.getUserDetails("Missing"));
        EngineException missingEvent = assertThrows(
                EngineException.class,
                () -> fixture.engine.getMarketEventDetails(99));
        EngineException inactivePurchase = assertThrows(
                EngineException.class,
                () -> fixture.engine.purchaseShares(1, "Buyer", 1, 1L));

        assertEquals(ErrorCode.USER_NOT_FOUND, missingUser.getErrorCode());
        assertEquals(ErrorCode.EVENT_NOT_FOUND, missingEvent.getErrorCode());
        assertEquals(ErrorCode.EVENT_NOT_STARTED, inactivePurchase.getErrorCode());
    }

    @Test
    void failedDomainPurchaseIsNotRetriedOrFollowedByEngineMutation() {
        CountingFailingMechanism mechanism = new CountingFailingMechanism();
        Fixture fixture = createFixture(
                CommissionType.ON_PURCHASE, 1_000.0, mechanism);
        fixture.engine.openEvent(1, "Maker");
        double buyerBalance = fixture.buyer.getBalance();
        double makerBalance = fixture.marketMaker.getBalance();
        double eventBalance = fixture.event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.engine.purchaseShares(1, "Buyer", 1, 1L));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(1, mechanism.purchaseAttempts);
        assertEquals(buyerBalance, fixture.buyer.getBalance());
        assertEquals(makerBalance, fixture.marketMaker.getBalance());
        assertEquals(eventBalance, fixture.event.getAccount().getBalance());
        assertFalse(fixture.buyer.hasPosition(1));
        assertEquals(0L, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(0, fixture.event.getTrades().size());
    }

    @Test
    void assignmentOneMethodsRemainAvailableAndUseLegacyBehavior() {
        MarketSystem system = new MarketSystem();
        MarketEvent event = new MarketEvent(
                1,
                "Legacy event",
                "Description",
                options(),
                new CommissionPolicy(10, CommissionType.ON_CLOSE),
                new LmsrTradingMechanism(10, new LmsrCalculator()),
                20.0);
        system.addEvent(event);
        GuessMarketEngine engine = new GuessMarketEngineImpl(system);

        assertTrue(engine.isSystemLoaded());
        assertEquals(1, engine.getAllEvents().size());
        assertEquals(1, engine.getActiveEvents().size());
        assertEquals(1, engine.getEventDetails(1).eventId());
        PurchaseResult purchase = engine.purchaseShares(1, 1, 2L);
        CloseEventResult close = engine.closeEvent(1, 1);
        EngineException invalidPath = assertThrows(
                EngineException.class,
                () -> engine.loadSystem(null));

        assertEquals(2L, purchase.shareQuantity());
        assertEquals(EventStatus.CLOSED, close.closedEvent().status());
        assertEquals(ErrorCode.INVALID_FILE_PATH, invalidPath.getErrorCode());
        assertTrue(engine.isSystemLoaded());
    }

    @Test
    void testConstructorRejectsNullSystem() {
        assertThrows(NullPointerException.class, () -> new GuessMarketEngineImpl(null));
    }

    private static void assertInvalidUserName(Action action) {
        EngineException exception = assertThrows(EngineException.class, action::run);
        assertEquals(ErrorCode.INVALID_USER_NAME, exception.getErrorCode());
    }

    private static void assertNoSystemLoaded(Action action) {
        EngineException exception = assertThrows(EngineException.class, action::run);
        assertEquals(ErrorCode.NO_SYSTEM_LOADED, exception.getErrorCode());
    }

    private static Fixture createFixture(
            CommissionType commissionType,
            double buyerBalance) {
        return createFixture(
                commissionType,
                buyerBalance,
                new LmsrTradingMechanism(10, new LmsrCalculator()));
    }

    private static Fixture createFixture(
            CommissionType commissionType,
            double buyerBalance,
            LmsrTradingOperations mechanism) {
        MarketSystem system = new MarketSystem();
        User marketMaker = new User("Maker", 1_000.0);
        User buyer = new User("Buyer", buyerBalance);
        User observer = new User("Observer", 1_000.0);
        MarketEvent event = createNotStartedEvent(1, commissionType, mechanism);
        system.addUser(marketMaker);
        system.addUser(buyer);
        system.addUser(observer);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        return new Fixture(
                system,
                event,
                marketMaker,
                buyer,
                new GuessMarketEngineImpl(system));
    }

    private static MarketEvent createNotStartedEvent(
            int eventId,
            CommissionType commissionType,
            LmsrTradingOperations mechanism) {
        return MarketEvent.createNotStartedEvent(
                eventId,
                "Event " + eventId,
                "Description",
                options(),
                new CommissionPolicy(10, commissionType),
                mechanism);
    }

    private static List<MarketOption> options() {
        return List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No"));
    }

    private record Fixture(
            MarketSystem system,
            MarketEvent event,
            User marketMaker,
            User buyer,
            GuessMarketEngine engine) {
    }

    @FunctionalInterface
    private interface Action {
        void run();
    }

    private static final class CountingFailingMechanism implements LmsrTradingOperations {
        private int purchaseAttempts;

        @Override
        public double calculatePurchaseCost(
                List<MarketOption> options,
                int optionNumber,
                long quantity) {
            purchaseAttempts++;
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "Controlled quote failure.");
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
