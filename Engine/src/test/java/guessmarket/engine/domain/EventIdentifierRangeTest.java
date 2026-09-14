package guessmarket.engine.domain;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.trading.TradingMechanism;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventIdentifierRangeTest {
    private static final List<Integer> EVENT_IDS = List.of(
            1,
            0,
            -1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE);

    @Test
    void marketPositionAcceptsTheCompleteIntegerRange() {
        for (int eventId : EVENT_IDS) {
            assertEquals(eventId, new MarketPosition(eventId).getEventId());
        }
    }

    @Test
    void userAccountRecordsAndRetrievesPositionsAcrossTheIntegerRange() {
        UserAccount account = new UserAccount(100.0);

        for (int index = 0; index < EVENT_IDS.size(); index++) {
            int eventId = EVENT_IDS.get(index);
            account.recordExecutedPurchase(eventId, 1, index + 1L, index + 1.0);
        }

        for (int index = 0; index < EVENT_IDS.size(); index++) {
            int eventId = EVENT_IDS.get(index);
            assertEquals(index + 1L, account.getSharesForOption(eventId, 1));
            assertEquals(index + 1.0, account.getAmountPaidForOption(eventId, 1));
        }
        assertEquals(Set.copyOf(EVENT_IDS), account.getPositionEventIds());
    }

    @Test
    void userDelegatesPositionsForZeroNegativeAndBoundaryEventIds() {
        User user = new User("Alice", 100.0);

        for (int eventId : EVENT_IDS) {
            user.recordExecutedPurchase(eventId, 1, 1L, 2.0);
            assertEquals(1L, user.getSharesForOption(eventId, 1));
            assertEquals(2.0, user.getAmountPaidForOption(eventId, 1));
        }

        assertEquals(Set.copyOf(EVENT_IDS), user.getPositionEventIds());
    }

    @Test
    void negativeAndPositiveEventIdsRemainDistinctPositionKeys() {
        UserAccount account = new UserAccount(100.0);

        account.recordExecutedPurchase(-1, 1, 2L, 3.0);
        account.recordExecutedPurchase(1, 1, 5L, 7.0);

        assertEquals(2L, account.getSharesForOption(-1, 1));
        assertEquals(5L, account.getSharesForOption(1, 1));
        assertEquals(Set.of(-1, 1), account.getPositionEventIds());
    }

    @Test
    void transactionValueObjectsPreserveEveryIntegerEventId() {
        for (int eventId : EVENT_IDS) {
            PurchaseQuote quote = PurchaseQuote.create(eventId, 1, 1L, 2.0, 0.5);
            SettlementPlan plan = settlementPlan(eventId);
            SettlementOutcome outcome = SettlementOutcome.from(plan);

            assertEquals(eventId, quote.eventId());
            assertEquals(eventId, plan.eventId());
            assertEquals(eventId, outcome.eventId());
        }
    }

    @Test
    void marketSystemStoresAndLooksUpZeroNegativeAndBoundaryEventIds() {
        MarketSystem system = new MarketSystem();

        for (int eventId : EVENT_IDS) {
            MarketEvent event = createEvent(eventId);
            system.addEvent(event);
            assertSame(event, system.getEvent(eventId));
        }

        assertEquals(EVENT_IDS, system.getAllEvents().stream()
                .map(MarketEvent::getId)
                .toList());
    }

    @Test
    void optionNumbersRemainPositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PurchaseQuote.create(0, 0, 1L, 2.0, 0.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> PurchaseQuote.create(-1, -1, 1L, 2.0, 0.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> settlementPlan(0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> settlementPlan(-1, -1));
    }

    private static SettlementPlan settlementPlan(int eventId) {
        return settlementPlan(eventId, 1);
    }

    private static SettlementPlan settlementPlan(int eventId, int winningOptionNumber) {
        return new SettlementPlan(
                eventId,
                winningOptionNumber,
                "Maker",
                0.0,
                1.0,
                List.of(),
                List.of(),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0);
    }

    private static MarketEvent createEvent(int eventId) {
        TradingMechanism mechanism = () -> TradingMethod.LMSR;
        return new MarketEvent(
                eventId,
                "Event " + eventId,
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                mechanism,
                10.0);
    }
}
