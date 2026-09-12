package guessmarket.engine.domain;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketEventLifecycleTest {
    @Test
    void legacyEventStartsActiveWithItsExistingFunding() {
        MarketEvent event = createLegacyEvent(1, 125.0);

        assertEquals(EventStatus.ACTIVE, event.getStatus());
        assertEquals(125.0, event.getAccount().getBalance());
    }

    @Test
    void assignmentTwoEventStartsNotStartedWithoutFunding() {
        MarketEvent event = createNotStartedEvent(1);

        assertEquals(EventStatus.NOT_STARTED, event.getStatus());
        assertEquals(0.0, event.getAccount().getBalance());
    }

    @Test
    void requiredSubsidyComesFromTheLmsrMechanism() {
        LmsrTradingMechanism mechanism = createMechanism();
        MarketEvent event = createNotStartedEvent(1, mechanism);

        assertEquals(mechanism.calculateInitialSubsidy(), event.getRequiredInitialSubsidy());
    }

    @Test
    void legacyAndNotStartedCreationPathsDoNotShareState() {
        MarketEvent legacy = createLegacyEvent(1, 125.0);
        MarketEvent notStarted = createNotStartedEvent(2);

        legacy.purchase(1, 2L);

        assertNotSame(legacy.getAccount(), notStarted.getAccount());
        assertEquals(0L, notStarted.findOption(1).getPurchasedShares());
        assertEquals(0.0, notStarted.getAccount().getBalance());
        assertEquals(EventStatus.NOT_STARTED, notStarted.getStatus());
    }

    @Test
    void cannotPurchaseBeforeEventStarts() {
        MarketEvent event = createNotStartedEvent(1);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.purchase(1, 1L));

        assertEquals(ErrorCode.EVENT_NOT_STARTED, exception.getErrorCode());
        assertEquals(0L, event.findOption(1).getPurchasedShares());
        assertEquals(0.0, event.getAccount().getBalance());
    }

    @Test
    void cannotCloseBeforeEventStarts() {
        MarketEvent event = createNotStartedEvent(1);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> event.close(1));

        assertEquals(ErrorCode.EVENT_NOT_STARTED, exception.getErrorCode());
        assertEquals(EventStatus.NOT_STARTED, event.getStatus());
        assertEquals(0.0, event.getAccount().getBalance());
    }

    private static MarketEvent createLegacyEvent(int eventId, double initialSubsidy) {
        return new MarketEvent(
                eventId,
                "Event " + eventId,
                "Description",
                createOptions(),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                createMechanism(),
                initialSubsidy);
    }

    private static MarketEvent createNotStartedEvent(int eventId) {
        return createNotStartedEvent(eventId, createMechanism());
    }

    private static MarketEvent createNotStartedEvent(
            int eventId,
            LmsrTradingMechanism mechanism) {
        return MarketEvent.createNotStartedEvent(
                eventId,
                "Event " + eventId,
                "Description",
                createOptions(),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                mechanism);
    }

    private static List<MarketOption> createOptions() {
        return List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No"));
    }

    private static LmsrTradingMechanism createMechanism() {
        return new LmsrTradingMechanism(10, new LmsrCalculator());
    }
}
