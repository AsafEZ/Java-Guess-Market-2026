package guessmarket.engine.domain;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.WinningPayoutOperations;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementExecutionTest {
    private static final double TOLERANCE = 1.0e-12;

    @Test
    void closesSingleWinnerAndDistributesEntireEventBalance() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L, 3.0, 0.0);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(3.0, fixture.alice.getBalance() - 100.0, TOLERANCE);
        assertEquals(7.0, fixture.marketMaker.getBalance() - 100.0, TOLERANCE);
        assertEquals(3.0, outcome.totalGrossPayout());
        assertEquals(3.0, outcome.totalWinnerNetPayout());
        assertEquals(7.0, outcome.marketMakerResidual());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
        assertEquals(0.0, outcome.eventBalanceAfter());
        assertEquals(EventStatus.CLOSED, fixture.event.getStatus());
        assertEquals(1, fixture.event.getWinningOptionNumber());
    }

    @Test
    void closesMultipleWinnersWithNetPayoutsAndMarketMakerEntitlement() {
        SettlementFixture fixture = createFixture(
                10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L, 2.0, 0.0);
        recordHolding(fixture, fixture.bob, 1, 3L, 3.0, 0.0);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(101.8, fixture.alice.getBalance(), TOLERANCE);
        assertEquals(102.7, fixture.bob.getBalance(), TOLERANCE);
        assertEquals(105.5, fixture.marketMaker.getBalance(), TOLERANCE);
        assertEquals(0.5, outcome.totalClosingCommission(), TOLERANCE);
        assertEquals(5.0, outcome.marketMakerResidual(), TOLERANCE);
        assertEquals(5.5, outcome.totalMarketMakerCredit(), TOLERANCE);
        assertEquals(3, outcome.accountCredits().size());
    }

    @Test
    void marketMakerWinnerReceivesOneConsolidatedCredit() {
        SettlementFixture fixture = createFixture(
                10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.marketMaker, 1, 2L, 2.0, 0.0);
        recordHolding(fixture, fixture.alice, 1, 3L, 3.0, 0.0);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(1L, outcome.accountCredits().stream()
                .filter(credit -> credit.userName().equals("Maker"))
                .count());
        assertEquals(107.3, fixture.marketMaker.getBalance(), TOLERANCE);
        assertEquals(102.7, fixture.alice.getBalance(), TOLERANCE);
        assertEquals(7.3, creditFor(outcome, "Maker"), TOLERANCE);
    }

    @Test
    void closingCommissionAccumulatesWithoutChangingSharesOrAmountPaid() {
        SettlementFixture fixture = createFixture(
                20, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L, 6.0, 0.4);

        fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(2L, fixture.alice.getSharesForOption(1, 1));
        assertEquals(6.0, fixture.alice.getAmountPaidForOption(1, 1));
        assertEquals(0.8, fixture.alice.getCommissionPaidForOption(1, 1), TOLERANCE);
        assertEquals(0.0, fixture.event.getAccount().getTotalCommissionCollected());
    }

    @Test
    void nonClosingCommissionDoesNotChangePositionCommission() {
        SettlementFixture fixture = createFixture(
                20, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L, 6.0, 0.4);

        fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(0.4, fixture.alice.getCommissionPaidForOption(1, 1));
    }

    @Test
    void blockedWinnerReceivesPassiveCreditAndRemainsBlocked() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        fixture.alice.debit(101.0);
        recordHolding(fixture, fixture.alice, 1, 2L, 2.0, 0.0);

        fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(1.0, fixture.alice.getBalance());
        assertEquals(UserStatus.BLOCKED, fixture.alice.getStatus());
    }

    @Test
    void outcomeMatchesFreshPlanAndExposesImmutableSnapshots() {
        SettlementFixture fixture = createFixture(
                10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L, 3.0, 0.0);
        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        assertEquals(plan.eventId(), outcome.eventId());
        assertEquals(plan.winningOptionNumber(), outcome.winningOptionNumber());
        assertEquals(plan.marketMakerName(), outcome.marketMakerName());
        assertEquals(plan.userSettlements(), outcome.userSettlements());
        assertEquals(plan.accountCredits(), outcome.accountCredits());
        assertEquals(plan.totalGrossPayout(), outcome.totalGrossPayout());
        assertEquals(plan.totalClosingCommission(), outcome.totalClosingCommission());
        assertEquals(plan.totalWinnerNetPayout(), outcome.totalWinnerNetPayout());
        assertEquals(plan.marketMakerResidual(), outcome.marketMakerResidual());
        assertEquals(plan.totalMarketMakerCredit(), outcome.totalMarketMakerCredit());
        assertEquals(plan.eventBalanceBefore(), outcome.eventBalanceBefore());
        assertEquals(0.0, outcome.eventBalanceAfter());
        assertTrue(SettlementOutcome.class.isRecord());
        assertTrue(Modifier.isFinal(SettlementOutcome.class.getModifiers()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> outcome.userSettlements().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> outcome.accountCredits().clear());
    }

    @Test
    void repeatedCloseIsRejectedWithoutSecondPayoutOrCommission() {
        SettlementFixture fixture = createFixture(
                10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L, 3.0, 0.2);
        fixture.system.closeEvent(1, "Maker", 1);
        DomainState settledState = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.EVENT_ALREADY_CLOSED, exception.getErrorCode());
        assertEquals(settledState, captureState(fixture));
    }

    @Test
    void winnerCreditOverflowRejectsBeforeAnyMutation() {
        SettlementFixture fixture = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                Double.MAX_VALUE / 2.0,
                new FixedPayoutMechanism(Double.MAX_VALUE / 2.0),
                100.0,
                Double.MAX_VALUE,
                100.0);
        recordHolding(fixture, fixture.alice, 1, 1L, 1.0, 0.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void marketMakerCreditOverflowRejectsBeforeAnyMutation() {
        SettlementFixture fixture = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                Double.MAX_VALUE,
                lmsrMechanism(),
                Double.MAX_VALUE,
                100.0,
                100.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void commissionBookkeepingOverflowRejectsBeforeAnyMutation() {
        double payout = Double.MAX_VALUE / 2.0;
        SettlementFixture fixture = createFixture(
                90,
                CommissionType.ON_CLOSE,
                payout,
                new FixedPayoutMechanism(payout),
                100.0,
                100.0,
                100.0);
        recordHolding(
                fixture, fixture.alice, 1, 1L, 1.0, Double.MAX_VALUE);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void positionMismatchRejectsWithoutMutation() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        fixture.alice.recordExecutedPurchase(1, 1, 2L, 2.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.POSITION_AGGREGATE_MISMATCH, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void insufficientEventFundsRejectsWithoutPartialPayout() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 1.0);
        recordHolding(fixture, fixture.alice, 1, 2L, 2.0, 0.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.INSUFFICIENT_EVENT_FUNDS, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void nonMarketMakerIsRejectedWithoutMutation() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Alice", 1));

        assertEquals(ErrorCode.USER_NOT_MARKET_MAKER, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void blockedMarketMakerIsRejectedWithoutMutation() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        fixture.marketMaker.debit(101.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.USER_ACCOUNT_BLOCKED, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void notStartedEventIsRejectedWithoutMutation() {
        MarketSystem system = new MarketSystem();
        User maker = new User("Maker", 100.0);
        MarketEvent event = MarketEvent.createNotStartedEvent(
                1,
                "Event 1",
                "Description",
                options(),
                new CommissionPolicy(0, CommissionType.ON_PURCHASE),
                lmsrMechanism());
        system.addUser(maker);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.EVENT_NOT_STARTED, exception.getErrorCode());
        assertEquals(EventStatus.NOT_STARTED, event.getStatus());
        assertEquals(0.0, event.getAccount().getBalance());
        assertNull(event.getWinningOptionNumber());
    }

    @Test
    void invalidWinningOptionIsRejectedWithoutMutation() {
        SettlementFixture fixture = createFixture(
                0, CommissionType.ON_PURCHASE, 10.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 3));

        assertEquals(ErrorCode.INVALID_OPTION_NUMBER, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void changedPayoutCapabilityIsRejectedBeforeMutation() {
        SettlementFixture fixture = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                10.0,
                new ChangingPayoutMechanism(),
                100.0,
                100.0,
                100.0);
        recordHolding(fixture, fixture.alice, 1, 1L, 1.0, 0.0);
        DomainState before = captureState(fixture);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.closeEvent(1, "Maker", 1));

        assertEquals(ErrorCode.SETTLEMENT_STATE_MISMATCH, exception.getErrorCode());
        assertEquals(before, captureState(fixture));
    }

    @Test
    void accountCreditsExactlyEqualDrainedEventBalance() {
        SettlementFixture fixture = createFixture(
                17, CommissionType.ON_CLOSE, 20.0);
        recordHolding(fixture, fixture.marketMaker, 1, 2L, 2.0, 0.0);
        recordHolding(fixture, fixture.alice, 1, 4L, 4.0, 0.0);
        recordHolding(fixture, fixture.bob, 1, 3L, 3.0, 0.0);

        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);
        double creditTotal = 0.0;
        for (AccountCredit credit : outcome.accountCredits()) {
            creditTotal += credit.amount();
        }

        assertEquals(outcome.eventBalanceBefore(), creditTotal);
        assertEquals(0.0, outcome.eventBalanceAfter());
        assertEquals(0.0, fixture.event.getAccount().getBalance());
    }

    @Test
    void eventAccountDrainRequiresExactBalanceAndPreservesLegacyCommissionTotal() {
        EventAccount account = new EventAccount(10.0);
        account.recordPurchase(2.0, 0.5);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> account.validateSettlementDrain(12.0));

        assertEquals(ErrorCode.SETTLEMENT_STATE_MISMATCH, exception.getErrorCode());
        assertEquals(12.5, account.getBalance());
        assertEquals(0.5, account.getTotalCommissionCollected());

        account.validateSettlementDrain(12.5);
        account.applyValidatedSettlementDrain();

        assertEquals(0.0, account.getBalance());
        assertEquals(0.5, account.getTotalCommissionCollected());
    }

    @Test
    void legacyClosePathStillUsesCloseOutcomeAndExistingAccounting() {
        MarketEvent event = new MarketEvent(
                1,
                "Legacy event",
                "Description",
                options(),
                new CommissionPolicy(10, CommissionType.ON_CLOSE),
                lmsrMechanism(),
                10.0);
        event.findOption(1).addShares(4L);

        CloseOutcome outcome = event.close(1);

        assertEquals(4.0, outcome.grossPayout());
        assertEquals(0.4, outcome.commission(), TOLERANCE);
        assertEquals(3.6, outcome.netPayout(), TOLERANCE);
        assertEquals(6.4, event.getAccount().getBalance(), TOLERANCE);
        assertEquals(EventStatus.CLOSED, event.getStatus());
    }

    private static double creditFor(SettlementOutcome outcome, String userName) {
        return outcome.accountCredits().stream()
                .filter(credit -> credit.userName().equals(userName))
                .findFirst()
                .orElseThrow()
                .amount();
    }

    private static void recordHolding(
            SettlementFixture fixture,
            User user,
            int optionNumber,
            long quantity,
            double paidAmount,
            double commissionPaid) {
        user.recordExecutedPurchase(
                1, optionNumber, quantity, paidAmount, commissionPaid);
        fixture.event.findOption(optionNumber).addShares(quantity);
    }

    private static DomainState captureState(SettlementFixture fixture) {
        return new DomainState(
                fixture.event.getAccount().getBalance(),
                fixture.event.getAccount().getTotalCommissionCollected(),
                fixture.event.getStatus(),
                fixture.event.getWinningOptionNumber(),
                fixture.event.getOptions().stream()
                        .map(MarketOption::getPurchasedShares)
                        .toList(),
                fixture.event.getTrades(),
                List.of(
                        captureUser(fixture.marketMaker),
                        captureUser(fixture.alice),
                        captureUser(fixture.bob)));
    }

    private static UserState captureUser(User user) {
        return new UserState(
                user.getBalance(),
                user.getStatus(),
                user.getSharesForOption(1, 1),
                user.getSharesForOption(1, 2),
                user.getAmountPaidForOption(1, 1),
                user.getAmountPaidForOption(1, 2),
                user.getCommissionPaidForOption(1, 1),
                user.getCommissionPaidForOption(1, 2),
                user.getPositionEventIds());
    }

    private static SettlementFixture createFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double eventBalance) {
        return createFixture(
                commissionPercentage,
                commissionType,
                eventBalance,
                lmsrMechanism(),
                100.0,
                100.0,
                100.0);
    }

    private static SettlementFixture createFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double eventBalance,
            TradingMechanism mechanism,
            double marketMakerBalance,
            double aliceBalance,
            double bobBalance) {
        MarketSystem system = new MarketSystem();
        User marketMaker = new User("Maker", marketMakerBalance);
        User alice = new User("Alice", aliceBalance);
        User bob = new User("Bob", bobBalance);
        MarketEvent event = new MarketEvent(
                1,
                "Event 1",
                "Description",
                options(),
                new CommissionPolicy(commissionPercentage, commissionType),
                mechanism,
                eventBalance);
        system.addUser(marketMaker);
        system.addUser(alice);
        system.addUser(bob);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        return new SettlementFixture(system, event, marketMaker, alice, bob);
    }

    private static List<MarketOption> options() {
        return List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No"));
    }

    private static LmsrTradingMechanism lmsrMechanism() {
        return new LmsrTradingMechanism(10, new LmsrCalculator());
    }

    private record SettlementFixture(
            MarketSystem system,
            MarketEvent event,
            User marketMaker,
            User alice,
            User bob) {
    }

    private record DomainState(
            double eventBalance,
            double eventCommission,
            EventStatus eventStatus,
            Integer winningOption,
            List<Long> aggregateShares,
            List<Trade> trades,
            List<UserState> users) {
    }

    private record UserState(
            double balance,
            UserStatus status,
            long optionOneShares,
            long optionTwoShares,
            double optionOneAmountPaid,
            double optionTwoAmountPaid,
            double optionOneCommission,
            double optionTwoCommission,
            Set<Integer> positionEventIds) {
    }

    private record FixedPayoutMechanism(double payoutPerWinningShare)
            implements TradingMechanism, WinningPayoutOperations {
        @Override
        public TradingMethod getTradingMethod() {
            return TradingMethod.LMSR;
        }

        @Override
        public double getPayoutPerWinningShare() {
            return payoutPerWinningShare;
        }
    }

    private static final class ChangingPayoutMechanism
            implements TradingMechanism, WinningPayoutOperations {
        private int calls;

        @Override
        public TradingMethod getTradingMethod() {
            return TradingMethod.LMSR;
        }

        @Override
        public double getPayoutPerWinningShare() {
            return ++calls == 1 ? 1.0 : 2.0;
        }
    }
}
