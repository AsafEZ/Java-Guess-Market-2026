package guessmarket.engine.impl;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.domain.CommissionPolicy;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.User;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.OrderBookEventDetails;
import guessmarket.engine.dto.OrderSubmissionResult;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.OrderStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.orderbook.OrderBookTradingMechanism;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookEngineApiTest {

    @Test
    void eventDetailsExposeOrderBookConfigurationWithoutDomainObjects() {
        Fixture fixture = fixture(true);

        MarketEventDetails details = fixture.engine.getMarketEventDetails(1);
        OrderBookEventDetails orderBook = assertInstanceOf(
                OrderBookEventDetails.class, details.mechanismDetails());

        assertEquals(TradingMethod.ORDER_BOOK, details.tradingMethod());
        assertTrue(orderBook.mintAllowed());
        assertEquals(10, orderBook.initialInvestment());
        assertEquals(1, orderBook.d());
        assertEquals(2, orderBook.options().size());
        assertTrue(orderBook.options().getFirst().buyOrders().isEmpty());
        assertTrue(orderBook.executionsNewestFirst().isEmpty());
    }

    @Test
    void pendingOrderAppearsInEventAndUserParticipationSnapshots() {
        Fixture fixture = openedFixture(false);

        OrderSubmissionResult result = fixture.engine.submitOrder(
                1, " Alice ", 1, OrderSide.BUY, 2L, 0.20);
        OrderBookEventDetails book = assertInstanceOf(
                OrderBookEventDetails.class,
                result.updatedEvent().mechanismDetails());
        PositionDetails participation = result.updatedSubmittingUser()
                .positions().getFirst();

        assertEquals(OrderStatus.OPEN, result.submittedOrder().status());
        assertTrue(result.executions().isEmpty());
        assertEquals("Alice", book.options().getFirst()
                .buyOrders().getFirst().userName());
        assertEquals(0L, participation.totalShares());
        assertEquals(0.0, participation.totalAmountPaid());
        assertTrue(result.updatedEvent().participantPositions().stream()
                .anyMatch(position -> position.userName().equals("Alice")));
    }

    @Test
    void matchedOrderReturnsExecutionAndUpdatedSnapshots() {
        Fixture fixture = openedFixture(false);
        fixture.engine.submitOrder(1, "Maker", 1, OrderSide.SELL, 2L, 0.40);

        OrderSubmissionResult result = fixture.engine.submitOrder(
                1, "Alice", 1, OrderSide.BUY, 2L, 0.50);
        OrderBookEventDetails book = assertInstanceOf(
                OrderBookEventDetails.class,
                result.updatedEvent().mechanismDetails());

        assertEquals(OrderStatus.FILLED, result.submittedOrder().status());
        assertEquals(1, result.executions().size());
        assertEquals("Maker", result.executions().getFirst()
                .sellerName().orElseThrow());
        assertEquals(0.40, result.executions().getFirst().unitPrice());
        assertEquals(2L, result.updatedSubmittingUser()
                .positions().getFirst().totalShares());
        assertEquals(0.40, book.options().getFirst().lastPrice().orElseThrow());
    }

    @Test
    void mintExecutionsAreProjectedWithoutFakeSellers() {
        Fixture fixture = openedFixture(true);
        fixture.engine.submitOrder(1, "Alice", 1, OrderSide.BUY, 1L, 0.70);

        OrderSubmissionResult result = fixture.engine.submitOrder(
                1, "Bob", 2, OrderSide.BUY, 1L, 0.40);

        assertEquals(2, result.executions().size());
        assertTrue(result.executions().stream().allMatch(execution -> execution.minted()
                && execution.sellerName().isEmpty()));
    }

    @Test
    void dtoCollectionsAreImmutableSnapshots() {
        Fixture fixture = openedFixture(false);
        OrderSubmissionResult before = fixture.engine.submitOrder(
                1, "Alice", 1, OrderSide.BUY, 1L, 0.20);

        fixture.engine.submitOrder(1, "Bob", 1, OrderSide.BUY, 1L, 0.10);

        OrderBookEventDetails book = assertInstanceOf(
                OrderBookEventDetails.class,
                before.updatedEvent().mechanismDetails());
        assertEquals(1, book.options().getFirst().buyOrders().size());
        assertThrows(UnsupportedOperationException.class, () -> book.options().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> book.options().getFirst().buyOrders().clear());
        assertThrows(UnsupportedOperationException.class, () -> before.executions().clear());
    }

    @Test
    void submitOrderPreservesPublicBoundaryValidation() {
        GuessMarketEngine unloaded = new GuessMarketEngineImpl();
        EngineException noSystem = assertThrows(
                EngineException.class,
                () -> unloaded.submitOrder(
                        1, null, 1, OrderSide.BUY, 1L, 0.20));
        assertEquals(ErrorCode.NO_SYSTEM_LOADED, noSystem.getErrorCode());

        Fixture fixture = openedFixture(false);
        EngineException invalidUser = assertThrows(
                EngineException.class,
                () -> fixture.engine.submitOrder(
                        1, "  ", 1, OrderSide.BUY, 1L, 0.20));
        assertEquals(ErrorCode.INVALID_USER_NAME, invalidUser.getErrorCode());
    }

    @Test
    void officialV2FileExposesOrderBookDetailsThroughProductionApi() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        Path xml = Path.of(Objects.requireNonNull(
                getClass().getResource("/assignment2/xml/valid/small.xml"))
                .toURI());

        engine.loadSystem(xml);
        int orderBookEventId = engine.getAllMarketEvents().stream()
                .filter(event -> event.tradingMethod() == TradingMethod.ORDER_BOOK)
                .findFirst()
                .orElseThrow()
                .eventId();
        MarketEventDetails details = engine.getMarketEventDetails(orderBookEventId);

        assertInstanceOf(OrderBookEventDetails.class, details.mechanismDetails());
    }

    private static Fixture openedFixture(boolean mintAllowed) {
        Fixture fixture = fixture(mintAllowed);
        fixture.engine.openEvent(1, "Maker");
        return fixture;
    }

    private static Fixture fixture(boolean mintAllowed) {
        MarketSystem system = new MarketSystem();
        User maker = new User("Maker", 1_000.0);
        User alice = new User("Alice", 100.0);
        User bob = new User("Bob", 100.0);
        MarketEvent event = MarketEvent.createNotStartedEvent(
                1,
                "Order Book event",
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(10, CommissionType.ON_PURCHASE),
                new OrderBookTradingMechanism(mintAllowed, 10, 1));
        system.addUser(maker);
        system.addUser(alice);
        system.addUser(bob);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        return new Fixture(new GuessMarketEngineImpl(system));
    }

    private record Fixture(GuessMarketEngine engine) {
    }
}
