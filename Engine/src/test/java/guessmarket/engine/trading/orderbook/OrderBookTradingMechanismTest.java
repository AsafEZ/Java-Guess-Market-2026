package guessmarket.engine.trading.orderbook;

import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.OrderStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTradingMechanismTest {

    @Test
    void unmatchedOrderRestsInBook() {
        OrderBookTradingMechanism book = book(false);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Alice", 1, OrderSide.BUY, 5L, 0.40);

        assertTrue(outcome.executions().isEmpty());
        assertEquals(OrderStatus.OPEN, outcome.submittedOrder().status());
        assertEquals(List.of(outcome.submittedOrder()), book.getOpenOrders(1));
        assertEquals(List.of("Alice"), book.getPendingParticipantNames().stream().toList());
    }

    @Test
    void crossingOrderExecutesAtRestingOrderPrice() {
        OrderBookTradingMechanism book = book(false);
        book.submitOrder("Seller", 1, OrderSide.SELL, 5L, 0.40);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Buyer", 1, OrderSide.BUY, 5L, 0.60);

        OrderExecution execution = outcome.executions().getFirst();
        assertEquals("Buyer", execution.buyerName());
        assertEquals("Seller", execution.sellerName().orElseThrow());
        assertEquals(0.40, execution.unitPrice());
        assertFalse(execution.minted());
        assertTrue(book.getOpenOrders(1).isEmpty());
    }

    @Test
    void betterPriceThenEarlierOrderHasPriority() {
        OrderBookTradingMechanism book = book(false);
        book.submitOrder("LaterPrice", 1, OrderSide.SELL, 2L, 0.40);
        book.submitOrder("BestFirst", 1, OrderSide.SELL, 1L, 0.30);
        book.submitOrder("BestSecond", 1, OrderSide.SELL, 1L, 0.30);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Buyer", 1, OrderSide.BUY, 4L, 0.50);

        assertEquals(
                List.of("BestFirst", "BestSecond", "LaterPrice"),
                outcome.executions().stream()
                        .map(execution -> execution.sellerName().orElseThrow())
                        .toList());
    }

    @Test
    void partialExecutionKeepsRemainingQuantity() {
        OrderBookTradingMechanism book = book(false);
        book.submitOrder("Seller", 1, OrderSide.SELL, 3L, 0.40);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Buyer", 1, OrderSide.BUY, 5L, 0.40);

        assertEquals(3L, outcome.executions().getFirst().quantity());
        assertEquals(2L, outcome.submittedOrder().remainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, outcome.submittedOrder().status());
        assertEquals("Buyer", book.getOpenOrders(1).getFirst().userName());
    }

    @Test
    void mintUsesRestingBidAndComplementaryIncomingPrice() {
        OrderBookTradingMechanism book = book(true);
        book.submitOrder("Alice", 1, OrderSide.BUY, 4L, 0.70);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Bob", 2, OrderSide.BUY, 3L, 0.60);

        assertEquals(2, outcome.executions().size());
        assertEquals(0.30, outcome.executions().get(0).unitPrice(), 0.0000001);
        assertEquals(0.70, outcome.executions().get(1).unitPrice(), 0.0000001);
        assertTrue(outcome.executions().stream().allMatch(OrderExecution::minted));
        assertEquals(1L, book.getOpenOrders(1).getFirst().remainingQuantity());
        assertTrue(book.getOpenOrders(2).isEmpty());
    }

    @Test
    void mintDoesNotRunWhenDisabledOrCombinedBidIsTooLow() {
        OrderBookTradingMechanism disabled = book(false);
        disabled.submitOrder("Alice", 1, OrderSide.BUY, 1L, 0.70);
        assertTrue(disabled.submitOrder(
                "Bob", 2, OrderSide.BUY, 1L, 0.60).executions().isEmpty());

        OrderBookTradingMechanism tooLow = book(true);
        tooLow.submitOrder("Alice", 1, OrderSide.BUY, 1L, 0.40);
        assertTrue(tooLow.submitOrder(
                "Bob", 2, OrderSide.BUY, 1L, 0.50).executions().isEmpty());
    }

    @Test
    void selfOrdersFollowTheSameMatchingRules() {
        OrderBookTradingMechanism book = book(false);
        book.submitOrder("Alice", 1, OrderSide.SELL, 2L, 0.30);

        OrderSubmissionOutcome outcome = book.submitOrder(
                "Alice", 1, OrderSide.BUY, 2L, 0.50);

        assertEquals(1, outcome.executions().size());
        assertEquals("Alice", outcome.executions().getFirst().buyerName());
        assertEquals("Alice", outcome.executions().getFirst().sellerName().orElseThrow());
        assertTrue(book.getOpenOrders(1).isEmpty());
    }

    @Test
    void invalidPriceAndOptionAreRejectedWithoutBookMutation() {
        OrderBookTradingMechanism book = book(false);

        EngineException price = assertThrows(
                EngineException.class,
                () -> book.submitOrder("Alice", 1, OrderSide.BUY, 1L, 1.0));
        EngineException option = assertThrows(
                EngineException.class,
                () -> book.submitOrder("Alice", 0, OrderSide.BUY, 1L, 0.5));

        assertEquals(ErrorCode.INVALID_ORDER_PRICE, price.getErrorCode());
        assertEquals(ErrorCode.INVALID_OPTION_NUMBER, option.getErrorCode());
        assertTrue(book.getOpenOrders(1).isEmpty());
        assertTrue(book.getExecutions().isEmpty());
    }

    @Test
    void initialInvestmentMustProduceWholeSharePairs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderBookTradingMechanism(true, 5, 2));
    }

    @Test
    void stalePreparedSubmissionIsRejected() {
        OrderBookTradingMechanism book = book(false);
        PreparedOrderSubmission stale = book.prepareOrder(
                "Alice", 1, OrderSide.BUY, 1L, 0.4);
        book.submitOrder("Bob", 1, OrderSide.BUY, 1L, 0.3);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> book.applyPreparedOrder(stale));

        assertEquals(ErrorCode.ORDER_BOOK_STATE_MISMATCH, exception.getErrorCode());
        assertEquals(1, book.getOpenOrders(1).size());
    }

    @Test
    void returnedCollectionsAreImmutableSnapshots() {
        OrderBookTradingMechanism book = book(false);
        List<LimitOrder> before = book.getOpenOrders(1);
        book.submitOrder("Alice", 1, OrderSide.BUY, 1L, 0.4);

        assertTrue(before.isEmpty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> book.getOpenOrders(1).clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> book.getPendingParticipantNames().clear());
    }

    @Test
    void marketStatisticsReflectOpenBookAndLatestExecution() {
        OrderBookTradingMechanism book = book(false);
        book.submitOrder("BidLow", 1, OrderSide.BUY, 1L, 0.20);
        book.submitOrder("BidHigh", 1, OrderSide.BUY, 1L, 0.30);
        book.submitOrder("AskHigh", 1, OrderSide.SELL, 1L, 0.60);
        book.submitOrder("AskLow", 1, OrderSide.SELL, 1L, 0.50);

        assertEquals(0.30, book.getBestBid(1).orElseThrow());
        assertEquals(0.50, book.getBestAsk(1).orElseThrow());
        assertEquals(0.40, book.getMidPrice(1).orElseThrow(), 0.0000001);
        assertEquals(0.20, book.getSpread(1).orElseThrow(), 0.0000001);
        assertTrue(book.getLastPrice(1).isEmpty());

        book.submitOrder("CrossingBuyer", 1, OrderSide.BUY, 1L, 0.50);

        assertEquals(0.50, book.getLastPrice(1).orElseThrow());
    }

    private static OrderBookTradingMechanism book(boolean mintAllowed) {
        return new OrderBookTradingMechanism(mintAllowed, 100, 1);
    }
}
