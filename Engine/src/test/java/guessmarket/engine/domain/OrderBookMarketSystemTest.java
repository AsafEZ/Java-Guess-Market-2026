package guessmarket.engine.domain;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.orderbook.OrderBookTradingMechanism;
import guessmarket.engine.trading.orderbook.OrderSubmissionOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookMarketSystemTest {
    private static final double TOLERANCE = 0.0000001;

    @Test
    void openingFundsEventAndCreditsInitialPairsToMarketMaker() {
        Fixture fixture = fixture(true, 100, 1, 10, 1_000.0);

        fixture.system.openEvent(1, "Maker");

        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
        assertEquals(900.0, fixture.maker.getBalance());
        assertEquals(100.0, fixture.event.getAccount().getBalance());
        assertEquals(100L, fixture.maker.getSharesForOption(1, 1));
        assertEquals(100L, fixture.maker.getSharesForOption(1, 2));
        assertEquals(50.0, fixture.maker.getAmountPaidForOption(1, 1));
        assertEquals(50.0, fixture.maker.getAmountPaidForOption(1, 2));
        assertEquals(100L, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(100L, fixture.event.findOption(2).getPurchasedShares());
    }

    @Test
    void zeroInitialInvestmentOpensWithoutCreatingPosition() {
        Fixture fixture = fixture(false, 0, 1, 0, 100.0);

        fixture.system.openEvent(1, "Maker");

        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
        assertEquals(100.0, fixture.maker.getBalance());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
        assertFalse(fixture.maker.hasPosition(1));
    }

    @Test
    void insufficientOpeningFundsLeaveAllStateUnchanged() {
        Fixture fixture = fixture(false, 100, 1, 0, 50.0);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.openEvent(1, "Maker"));

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, exception.getErrorCode());
        assertEquals(EventStatus.NOT_STARTED, fixture.event.getStatus());
        assertEquals(50.0, fixture.maker.getBalance());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
        assertFalse(fixture.maker.hasPosition(1));
    }

    @Test
    void normalMatchTransfersMoneySharesAndCommissionAtomically() {
        Fixture fixture = openedFixture(false, 100, 1, 10);
        fixture.system.submitOrder("Maker", 1, 1, OrderSide.SELL, 10L, 0.40);

        OrderSubmissionOutcome outcome = fixture.system.submitOrder(
                "Alice", 1, 1, OrderSide.BUY, 10L, 0.50);

        assertEquals(1, outcome.executions().size());
        assertEquals(95.6, fixture.alice.getBalance(), TOLERANCE);
        assertEquals(904.4, fixture.maker.getBalance(), TOLERANCE);
        assertEquals(100.0, fixture.event.getAccount().getBalance());
        assertEquals(10L, fixture.alice.getSharesForOption(1, 1));
        assertEquals(90L, fixture.maker.getSharesForOption(1, 1));
        assertEquals(4.0, fixture.alice.getAmountPaidForOption(1, 1));
        assertEquals(0.4, fixture.alice.getCommissionPaidForOption(1, 1));
        assertEquals(45.0, fixture.maker.getAmountPaidForOption(1, 1));
        assertEquals(100L, fixture.event.findOption(1).getPurchasedShares());
    }

    @Test
    void pendingBuyMarksBookParticipationWithoutMovingFunds() {
        Fixture fixture = openedFixture(false, 100, 1, 10);

        fixture.system.submitOrder("Alice", 1, 1, OrderSide.BUY, 2L, 0.20);

        assertEquals(100.0, fixture.alice.getBalance());
        assertFalse(fixture.alice.hasPosition(1));
        assertEquals(List.of("Alice"),
                fixture.book.getPendingParticipantNames().stream().toList());
    }

    @Test
    void mintFundsEventAndCreatesMatchingPositions() {
        Fixture fixture = openedFixture(true, 100, 1, 10);
        fixture.system.submitOrder("Alice", 1, 1, OrderSide.BUY, 4L, 0.70);

        OrderSubmissionOutcome outcome = fixture.system.submitOrder(
                "Bob", 1, 2, OrderSide.BUY, 3L, 0.60);

        assertEquals(2, outcome.executions().size());
        assertEquals(103.0, fixture.event.getAccount().getBalance(), TOLERANCE);
        assertEquals(97.69, fixture.alice.getBalance(), TOLERANCE);
        assertEquals(99.01, fixture.bob.getBalance(), TOLERANCE);
        assertEquals(900.30, fixture.maker.getBalance(), TOLERANCE);
        assertEquals(3L, fixture.alice.getSharesForOption(1, 1));
        assertEquals(3L, fixture.bob.getSharesForOption(1, 2));
        assertEquals(0.21, fixture.alice.getCommissionPaidForOption(1, 1), TOLERANCE);
        assertEquals(0.09, fixture.bob.getCommissionPaidForOption(1, 2), TOLERANCE);
        assertEquals(103L, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(103L, fixture.event.findOption(2).getPurchasedShares());
    }

    @Test
    void marketMakerBuyerTracksGrossCommissionButPaysNetShareCost() {
        Fixture fixture = openedFixture(true, 100, 1, 10);
        fixture.system.submitOrder("Alice", 1, 1, OrderSide.BUY, 1L, 0.70);

        fixture.system.submitOrder("Maker", 1, 2, OrderSide.BUY, 1L, 0.40);

        assertEquals(899.77, fixture.maker.getBalance(), TOLERANCE);
        assertEquals(0.03, fixture.maker.getCommissionPaidForOption(1, 2), TOLERANCE);
        assertEquals(101L, fixture.maker.getSharesForOption(1, 2));
    }

    @Test
    void oversellingAndReservedSharesAreRejectedWithoutMutation() {
        Fixture fixture = openedFixture(false, 10, 1, 0);
        fixture.system.submitOrder("Maker", 1, 1, OrderSide.SELL, 7L, 0.40);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.submitOrder(
                        "Maker", 1, 1, OrderSide.SELL, 4L, 0.50));

        assertEquals(ErrorCode.INSUFFICIENT_SHARES, exception.getErrorCode());
        assertEquals(10L, fixture.maker.getSharesForOption(1, 1));
        assertEquals(1, fixture.book.getOpenOrders(1).size());
    }

    @Test
    void activeBuyerMayCompleteDebitThatBlocksAccount() {
        Fixture fixture = openedFixture(false, 10, 1, 10);
        User lowBalance = new User("Low", 0.10);
        fixture.system.addUser(lowBalance);
        fixture.system.submitOrder("Maker", 1, 1, OrderSide.SELL, 1L, 0.40);

        fixture.system.submitOrder("Low", 1, 1, OrderSide.BUY, 1L, 0.40);

        assertEquals(-0.34, lowBalance.getBalance(), TOLERANCE);
        assertEquals(UserStatus.BLOCKED, lowBalance.getStatus());
        assertEquals(1L, lowBalance.getSharesForOption(1, 1));
    }

    @Test
    void positionOverflowRejectsEntireSubmissionBeforeBookOrMoneyMutation() {
        Fixture fixture = activeFixture(true, 10);
        fixture.alice.recordExecutedPurchase(1, 1, Long.MAX_VALUE, 1.0);
        fixture.event.findOption(1).addShares(Long.MAX_VALUE);
        fixture.system.submitOrder("Alice", 1, 1, OrderSide.BUY, 1L, 0.70);
        double aliceBalance = fixture.alice.getBalance();
        double bobBalance = fixture.bob.getBalance();
        double makerBalance = fixture.maker.getBalance();
        double eventBalance = fixture.event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.submitOrder(
                        "Bob", 1, 2, OrderSide.BUY, 1L, 0.40));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(aliceBalance, fixture.alice.getBalance());
        assertEquals(bobBalance, fixture.bob.getBalance());
        assertEquals(makerBalance, fixture.maker.getBalance());
        assertEquals(eventBalance, fixture.event.getAccount().getBalance());
        assertEquals(Long.MAX_VALUE, fixture.alice.getSharesForOption(1, 1));
        assertFalse(fixture.bob.hasPosition(1));
        assertEquals(1, fixture.book.getOpenOrders(1).size());
        assertTrue(fixture.book.getOpenOrders(2).isEmpty());
        assertTrue(fixture.book.getExecutions().isEmpty());
    }

    @Test
    void orderBookSettlementUsesDAndDrainsEventAccount() {
        Fixture fixture = openedFixture(false, 10, 1, 0);
        fixture.system.submitOrder("Maker", 1, 1, OrderSide.SELL, 2L, 0.40);
        fixture.system.submitOrder("Alice", 1, 1, OrderSide.BUY, 2L, 0.40);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(10.0, outcome.totalGrossPayout());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
        assertEquals(EventStatus.CLOSED, fixture.event.getStatus());
        assertEquals(101.2, fixture.alice.getBalance(), TOLERANCE);
        assertEquals(998.8, fixture.maker.getBalance(), TOLERANCE);
    }

    private static Fixture openedFixture(
            boolean mintAllowed,
            int initial,
            int d,
            int commission) {
        Fixture fixture = fixture(mintAllowed, initial, d, commission, 1_000.0);
        fixture.system.openEvent(1, "Maker");
        return fixture;
    }

    private static Fixture fixture(
            boolean mintAllowed,
            int initial,
            int d,
            int commission,
            double makerBalance) {
        MarketSystem system = new MarketSystem();
        User maker = new User("Maker", makerBalance);
        User alice = new User("Alice", 100.0);
        User bob = new User("Bob", 100.0);
        OrderBookTradingMechanism book = new OrderBookTradingMechanism(
                mintAllowed, initial, d);
        MarketEvent event = MarketEvent.createNotStartedEvent(
                1,
                "Order Book event",
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(commission, CommissionType.ON_PURCHASE),
                book);
        system.addUser(maker);
        system.addUser(alice);
        system.addUser(bob);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        return new Fixture(system, event, maker, alice, bob, book);
    }

    private static Fixture activeFixture(boolean mintAllowed, int commission) {
        MarketSystem system = new MarketSystem();
        User maker = new User("Maker", 1_000.0);
        User alice = new User("Alice", 100.0);
        User bob = new User("Bob", 100.0);
        OrderBookTradingMechanism book = new OrderBookTradingMechanism(
                mintAllowed, 0, 1);
        MarketEvent event = new MarketEvent(
                1,
                "Order Book event",
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(commission, CommissionType.ON_PURCHASE),
                book,
                0.0);
        system.addUser(maker);
        system.addUser(alice);
        system.addUser(bob);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        return new Fixture(system, event, maker, alice, bob, book);
    }

    private record Fixture(
            MarketSystem system,
            MarketEvent event,
            User maker,
            User alice,
            User bob,
            OrderBookTradingMechanism book) {
    }
}
