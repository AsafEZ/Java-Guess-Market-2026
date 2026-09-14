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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketEventOpeningTest {
    @Test
    void marketMakerFundsAndOpensNotStartedEvent() {
        MarketEvent event = createNotStartedEvent(1);
        double subsidy = event.getRequiredInitialSubsidy();
        MarketSystem system = systemWithEventAndMarketMaker(event, subsidy + 100.0);

        system.openEvent(1, "Alice");

        assertEquals(EventStatus.ACTIVE, event.getStatus());
        assertEquals(100.0, system.getUser("Alice").getBalance(), 1.0e-12);
        assertEquals(subsidy, event.getAccount().getBalance(), 1.0e-12);
    }

    @Test
    void differentUserCannotOpenEvent() {
        MarketEvent event = createNotStartedEvent(1);
        MarketSystem system = systemWithEventAndMarketMaker(event, 100.0);
        User bob = new User("Bob", 100.0);
        system.addUser(bob);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Bob"));

        assertEquals(ErrorCode.USER_NOT_MARKET_MAKER, exception.getErrorCode());
        assertUnopened(event, bob, 100.0);
    }

    @Test
    void blockedMarketMakerCannotOpenEvenWithEnoughBalance() {
        MarketEvent event = createNotStartedEvent(1);
        double subsidy = event.getRequiredInitialSubsidy();
        MarketSystem system = systemWithEventAndMarketMaker(event, subsidy + 10.0);
        User marketMaker = system.getUser("Alice");
        marketMaker.debit(subsidy + 11.0);
        marketMaker.credit(subsidy + 2.0);
        double blockedBalance = marketMaker.getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Alice"));

        assertEquals(ErrorCode.USER_ACCOUNT_BLOCKED, exception.getErrorCode());
        assertEquals(UserStatus.BLOCKED, marketMaker.getStatus());
        assertUnopened(event, marketMaker, blockedBalance);
    }

    @Test
    void insufficientFundsRejectOpeningWithoutDebitOrBlocking() {
        MarketEvent event = createNotStartedEvent(1);
        double initialBalance = event.getRequiredInitialSubsidy() / 2.0;
        MarketSystem system = systemWithEventAndMarketMaker(event, initialBalance);
        User marketMaker = system.getUser("Alice");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Alice"));

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, exception.getErrorCode());
        assertEquals(UserStatus.ACTIVE, marketMaker.getStatus());
        assertUnopened(event, marketMaker, initialBalance);
    }

    @Test
    void eventWithoutMarketMakerCannotBeOpened() {
        MarketEvent event = createNotStartedEvent(1);
        MarketSystem system = new MarketSystem();
        User user = new User("Alice", 100.0);
        system.addUser(user);
        system.addEvent(event);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Alice"));

        assertEquals(ErrorCode.MARKET_MAKER_NOT_ASSIGNED, exception.getErrorCode());
        assertUnopened(event, user, 100.0);
    }

    @Test
    void repeatedOpeningDoesNotDebitOrFundAgain() {
        MarketEvent event = createNotStartedEvent(1);
        MarketSystem system = systemWithEventAndMarketMaker(event, 100.0);
        system.openEvent(1, "Alice");
        User marketMaker = system.getUser("Alice");
        double balanceAfterOpening = marketMaker.getBalance();
        double eventBalanceAfterOpening = event.getAccount().getBalance();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Alice"));

        assertEquals(ErrorCode.EVENT_ALREADY_STARTED, exception.getErrorCode());
        assertEquals(balanceAfterOpening, marketMaker.getBalance());
        assertEquals(eventBalanceAfterOpening, event.getAccount().getBalance());
        assertEquals(EventStatus.ACTIVE, event.getStatus());
    }

    @Test
    void unexpectedOpeningFailureCompensatesUserDebit() {
        MarketEvent event = createNotStartedEvent(1, new ChangingSubsidyMechanism());
        MarketSystem system = systemWithEventAndMarketMaker(event, 100.0);
        User marketMaker = system.getUser("Alice");

        assertThrows(IllegalStateException.class, () -> system.openEvent(1, "Alice"));

        assertUnopened(event, marketMaker, 100.0);
        assertEquals(UserStatus.ACTIVE, marketMaker.getStatus());
    }

    @Test
    void legacyEventRemainsActiveAndDoesNotUseOpeningFlow() {
        LmsrTradingMechanism mechanism = createMechanism();
        double subsidy = mechanism.calculateInitialSubsidy();
        MarketEvent event = new MarketEvent(
                1,
                "Legacy event",
                "Description",
                createOptions(),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                mechanism,
                subsidy);
        MarketSystem system = systemWithEventAndMarketMaker(event, 100.0);
        User marketMaker = system.getUser("Alice");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Alice"));

        assertEquals(ErrorCode.EVENT_ALREADY_STARTED, exception.getErrorCode());
        assertEquals(100.0, marketMaker.getBalance());
        assertEquals(subsidy, event.getAccount().getBalance());
        assertEquals(1L, event.purchase(1, 1L).shareQuantity());
    }

    @Test
    void unknownUserIsRejectedWithoutChangingEvent() {
        MarketEvent event = createNotStartedEvent(1);
        MarketSystem system = new MarketSystem();
        system.addEvent(event);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(1, "Missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(EventStatus.NOT_STARTED, event.getStatus());
        assertEquals(0.0, event.getAccount().getBalance());
    }

    @Test
    void unknownEventIsRejectedBeforeUserLookup() {
        MarketSystem system = new MarketSystem();

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.openEvent(99, "Missing"));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    private static MarketSystem systemWithEventAndMarketMaker(
            MarketEvent event,
            double initialBalance) {
        MarketSystem system = new MarketSystem();
        system.addUser(new User("Alice", initialBalance));
        system.addEvent(event);
        system.assignMarketMaker(event.getId(), "Alice");
        return system;
    }

    private static MarketEvent createNotStartedEvent(int eventId) {
        return createNotStartedEvent(eventId, createMechanism());
    }

    private static MarketEvent createNotStartedEvent(
            int eventId,
            LmsrTradingOperations mechanism) {
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

    private static void assertUnopened(
            MarketEvent event,
            User user,
            double expectedUserBalance) {
        assertEquals(EventStatus.NOT_STARTED, event.getStatus());
        assertEquals(0.0, event.getAccount().getBalance());
        assertEquals(expectedUserBalance, user.getBalance(), 1.0e-12);
    }

    private static final class ChangingSubsidyMechanism implements LmsrTradingOperations {
        private int subsidyCalculationCount;

        @Override
        public double calculatePurchaseCost(
                List<MarketOption> options,
                int optionNumber,
                long quantity) {
            return 1.0;
        }

        @Override
        public double calculateOptionValue(List<MarketOption> options, int optionNumber) {
            return 0.5;
        }

        @Override
        public double calculateInitialSubsidy() {
            subsidyCalculationCount++;
            return subsidyCalculationCount == 1 ? 10.0 : 11.0;
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
