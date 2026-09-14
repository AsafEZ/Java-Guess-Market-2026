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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementPlanTest {
    private static final double TOLERANCE = 1.0e-12;

    @Test
    void createsPlanForSingleWinner() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(1, plan.eventId());
        assertEquals(1, plan.winningOptionNumber());
        assertEquals("Maker", plan.marketMakerName());
        assertEquals(10.0, plan.eventBalanceBefore());
        assertEquals(1.0, plan.payoutPerWinningShare());
        assertEquals(3.0, plan.totalGrossPayout());
        assertEquals(3.0, plan.totalWinnerNetPayout());
        assertEquals(7.0, plan.marketMakerResidual());
        assertEquals(List.of(new UserSettlement("Alice", 3L, 3.0, 0.0, 3.0)),
                plan.userSettlements());
    }

    @Test
    void createsSeparateSettlementsForMultipleWinners() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L);
        recordHolding(fixture, fixture.bob, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(List.of(
                new UserSettlement("Alice", 2L, 2.0, 0.0, 2.0),
                new UserSettlement("Bob", 3L, 3.0, 0.0, 3.0)),
                plan.userSettlements());
        assertEquals(5.0, plan.totalGrossPayout());
    }

    @Test
    void losingPositionsReceiveNoPayout() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L);
        recordHolding(fixture, fixture.loser, 2, 4L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(List.of("Alice"), plan.userSettlements().stream()
                .map(UserSettlement::userName)
                .toList());
        assertFalse(plan.accountCredits().stream()
                .anyMatch(credit -> credit.userName().equals("Loser")));
    }

    @Test
    void onCloseCommissionIsCalculatedForEachWinner() {
        SettlementFixture fixture = createFixture(10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L);
        recordHolding(fixture, fixture.bob, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertSettlement(plan.userSettlements().get(0), "Alice", 2L, 2.0, 0.2, 1.8);
        assertSettlement(plan.userSettlements().get(1), "Bob", 3L, 3.0, 0.3, 2.7);
        assertEquals(0.5, plan.totalClosingCommission(), TOLERANCE);
        assertEquals(4.5, plan.totalWinnerNetPayout(), TOLERANCE);
    }

    @Test
    void nonClosingCommissionTypeProducesNoClosingCommission() {
        SettlementFixture fixture = createFixture(25, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(0.0, plan.userSettlements().getFirst().closingCommission());
        assertEquals(0.0, plan.totalClosingCommission());
    }

    @Test
    void marketMakerReceivesClosingCommissionAndResidual() {
        SettlementFixture fixture = createFixture(10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 4L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(6.0, plan.marketMakerResidual(), TOLERANCE);
        assertEquals(0.4, plan.totalClosingCommission(), TOLERANCE);
        assertEquals(6.4, plan.totalMarketMakerCredit(), TOLERANCE);
        assertEquals(6.4, creditFor(plan, "Maker"), TOLERANCE);
    }

    @Test
    void marketMakerWinnerReceivesOneConsolidatedCredit() {
        SettlementFixture fixture = createFixture(10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.marketMaker, 1, 2L);
        recordHolding(fixture, fixture.alice, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(1L, plan.accountCredits().stream()
                .filter(credit -> credit.userName().equals("Maker"))
                .count());
        assertEquals(7.3, creditFor(plan, "Maker"), TOLERANCE);
        assertEquals(2.7, creditFor(plan, "Alice"), TOLERANCE);
    }

    @Test
    void blockedWinnerIsIncludedWithoutChangingBlockedState() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        fixture.alice.debit(101.0);
        recordHolding(fixture, fixture.alice, 1, 2L);
        double balanceBefore = fixture.alice.getBalance();

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals("Alice", plan.userSettlements().getFirst().userName());
        assertEquals(2.0, creditFor(plan, "Alice"));
        assertEquals(UserStatus.BLOCKED, fixture.alice.getStatus());
        assertEquals(balanceBefore, fixture.alice.getBalance());
    }

    @Test
    void blockedMarketMakerCannotPrepareSettlement() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        fixture.marketMaker.debit(101.0);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.USER_ACCOUNT_BLOCKED, exception.getErrorCode());
    }

    @Test
    void nonMarketMakerCannotPrepareSettlement() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Alice", 1));

        assertEquals(ErrorCode.USER_NOT_MARKET_MAKER, exception.getErrorCode());
    }

    @Test
    void eventWithoutMarketMakerCannotPrepareSettlement() {
        SettlementFixture fixture = createFixtureWithoutAssignment(
                0, CommissionType.ON_PURCHASE, 10.0, lmsrMechanism());

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.MARKET_MAKER_NOT_ASSIGNED, exception.getErrorCode());
    }

    @Test
    void nonActiveEventCannotPrepareSettlement() {
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
                () -> system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.EVENT_NOT_STARTED, exception.getErrorCode());
    }

    @Test
    void invalidWinningOptionIsRejected() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 3));

        assertEquals(ErrorCode.INVALID_OPTION_NUMBER, exception.getErrorCode());
    }

    @Test
    void aggregateMismatchInFirstOptionIsRejected() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        fixture.alice.recordExecutedPurchase(1, 1, 2L, 1.0);

        assertAggregateMismatch(fixture);
    }

    @Test
    void aggregateMismatchInSecondOptionIsRejected() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        fixture.loser.recordExecutedPurchase(1, 2, 2L, 1.0);

        assertAggregateMismatch(fixture);
    }

    @Test
    void positionShareSummationOverflowIsRejected() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, Double.MAX_VALUE);
        fixture.alice.recordExecutedPurchase(1, 1, Long.MAX_VALUE, 1.0);
        fixture.bob.recordExecutedPurchase(1, 1, 1L, 1.0);
        fixture.event.findOption(1).addShares(Long.MAX_VALUE);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, exception.getErrorCode());
    }

    @Test
    void insufficientEventFundsRejectPlan() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 1.0);
        recordHolding(fixture, fixture.alice, 1, 2L);

        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.INSUFFICIENT_EVENT_FUNDS, exception.getErrorCode());
        assertEquals(1.0, fixture.event.getAccount().getBalance());
        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
    }

    @Test
    void overflowAndNonFinitePayoutsAreRejected() {
        SettlementFixture overflow = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                Double.MAX_VALUE,
                new FixedPayoutMechanism(Double.MAX_VALUE / 2.0));
        recordHolding(overflow, overflow.alice, 1, 3L);
        SettlementFixture nonFinite = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                10.0,
                new FixedPayoutMechanism(Double.NaN));

        EngineException overflowFailure = assertThrows(
                EngineException.class,
                () -> overflow.system.prepareSettlement(1, "Maker", 1));
        EngineException nonFiniteFailure = assertThrows(
                EngineException.class,
                () -> nonFinite.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, overflowFailure.getErrorCode());
        assertEquals(ErrorCode.ARITHMETIC_OVERFLOW, nonFiniteFailure.getErrorCode());
        assertEquals(EventStatus.ACTIVE, overflow.event.getStatus());
        assertEquals(EventStatus.ACTIVE, nonFinite.event.getStatus());
    }

    @Test
    void consolidatedCreditsConserveEntireEventBalance() {
        SettlementFixture fixture = createFixture(17, CommissionType.ON_CLOSE, 20.0);
        recordHolding(fixture, fixture.marketMaker, 1, 2L);
        recordHolding(fixture, fixture.alice, 1, 4L);
        recordHolding(fixture, fixture.bob, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);
        double creditTotal = 0.0;
        for (AccountCredit credit : plan.accountCredits()) {
            creditTotal += credit.amount();
        }

        assertEquals(plan.eventBalanceBefore(), creditTotal);
        assertEquals(
                plan.eventBalanceBefore(),
                plan.totalWinnerNetPayout()
                        + plan.totalClosingCommission()
                        + plan.marketMakerResidual(),
                TOLERANCE);
    }

    @Test
    void planUsesImmutableRecordsAndCollectionSnapshots() {
        SettlementFixture fixture = createFixture(0, CommissionType.ON_PURCHASE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 2L);
        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertTrue(SettlementPlan.class.isRecord());
        assertTrue(UserSettlement.class.isRecord());
        assertTrue(AccountCredit.class.isRecord());
        assertTrue(Modifier.isFinal(SettlementPlan.class.getModifiers()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> plan.userSettlements().add(
                        new UserSettlement("Bob", 1L, 1.0, 0.0, 1.0)));
        assertThrows(
                UnsupportedOperationException.class,
                () -> plan.accountCredits().add(new AccountCredit("Bob", 1.0)));
    }

    @Test
    void repeatedPreparationIsEqualAndDoesNotMutateDomainState() {
        SettlementFixture fixture = createFixture(10, CommissionType.ON_CLOSE, 10.0);
        recordHolding(fixture, fixture.alice, 1, 3L);
        recordHolding(fixture, fixture.loser, 2, 2L);
        double makerBalance = fixture.marketMaker.getBalance();
        double aliceBalance = fixture.alice.getBalance();
        double eventBalance = fixture.event.getAccount().getBalance();
        long winningShares = fixture.event.findOption(1).getPurchasedShares();
        long losingShares = fixture.event.findOption(2).getPurchasedShares();
        double commissionBefore = fixture.alice.getCommissionPaidForOption(1, 1);
        int tradeCount = fixture.event.getTrades().size();

        SettlementPlan first = fixture.system.prepareSettlement(1, "Maker", 1);
        SettlementPlan second = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(first, second);
        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
        assertNull(fixture.event.getWinningOptionNumber());
        assertEquals(eventBalance, fixture.event.getAccount().getBalance());
        assertEquals(makerBalance, fixture.marketMaker.getBalance());
        assertEquals(aliceBalance, fixture.alice.getBalance());
        assertEquals(UserStatus.ACTIVE, fixture.marketMaker.getStatus());
        assertEquals(UserStatus.ACTIVE, fixture.alice.getStatus());
        assertEquals(winningShares, fixture.event.findOption(1).getPurchasedShares());
        assertEquals(losingShares, fixture.event.findOption(2).getPurchasedShares());
        assertEquals(3L, fixture.alice.getSharesForOption(1, 1));
        assertEquals(commissionBefore, fixture.alice.getCommissionPaidForOption(1, 1));
        assertEquals(tradeCount, fixture.event.getTrades().size());
    }

    @Test
    void legacyClosePathRemainsUnchanged() {
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
        assertEquals(EventStatus.CLOSED, event.getStatus());
        assertEquals(1, event.getWinningOptionNumber());
        assertEquals(6.4, event.getAccount().getBalance(), TOLERANCE);
    }

    @Test
    void payoutCapabilitySupportsFutureOrderBookValueWithoutConcreteCast() {
        SettlementFixture fixture = createFixture(
                0,
                CommissionType.ON_PURCHASE,
                20.0,
                new FixedPayoutMechanism(5.0));
        recordHolding(fixture, fixture.alice, 1, 3L);

        SettlementPlan plan = fixture.system.prepareSettlement(1, "Maker", 1);

        assertEquals(5.0, plan.payoutPerWinningShare());
        assertEquals(15.0, plan.totalGrossPayout());
        assertEquals(5.0, plan.marketMakerResidual());
    }

    private static void assertAggregateMismatch(SettlementFixture fixture) {
        EngineException exception = assertThrows(
                EngineException.class,
                () -> fixture.system.prepareSettlement(1, "Maker", 1));

        assertEquals(ErrorCode.POSITION_AGGREGATE_MISMATCH, exception.getErrorCode());
        assertEquals(EventStatus.ACTIVE, fixture.event.getStatus());
        assertNull(fixture.event.getWinningOptionNumber());
        assertEquals(10.0, fixture.event.getAccount().getBalance());
    }

    private static void assertSettlement(
            UserSettlement settlement,
            String userName,
            long shares,
            double gross,
            double commission,
            double net) {
        assertEquals(userName, settlement.userName());
        assertEquals(shares, settlement.winningShares());
        assertEquals(gross, settlement.grossPayout(), TOLERANCE);
        assertEquals(commission, settlement.closingCommission(), TOLERANCE);
        assertEquals(net, settlement.netPayout(), TOLERANCE);
    }

    private static double creditFor(SettlementPlan plan, String userName) {
        return plan.accountCredits().stream()
                .filter(credit -> credit.userName().equals(userName))
                .findFirst()
                .orElseThrow()
                .amount();
    }

    private static void recordHolding(
            SettlementFixture fixture,
            User user,
            int optionNumber,
            long quantity) {
        user.recordExecutedPurchase(1, optionNumber, quantity, 1.0);
        fixture.event.findOption(optionNumber).addShares(quantity);
    }

    private static SettlementFixture createFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double eventBalance) {
        return createFixture(
                commissionPercentage,
                commissionType,
                eventBalance,
                lmsrMechanism());
    }

    private static SettlementFixture createFixture(
            int commissionPercentage,
            CommissionType commissionType,
            double eventBalance,
            TradingMechanism mechanism) {
        SettlementFixture fixture = createFixtureWithoutAssignment(
                commissionPercentage,
                commissionType,
                eventBalance,
                mechanism);
        fixture.system.assignMarketMaker(1, "Maker");
        return fixture;
    }

    private static SettlementFixture createFixtureWithoutAssignment(
            int commissionPercentage,
            CommissionType commissionType,
            double eventBalance,
            TradingMechanism mechanism) {
        MarketSystem system = new MarketSystem();
        User marketMaker = new User("Maker", 100.0);
        User alice = new User("Alice", 100.0);
        User bob = new User("Bob", 100.0);
        User loser = new User("Loser", 100.0);
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
        system.addUser(loser);
        system.addEvent(event);
        return new SettlementFixture(system, event, marketMaker, alice, bob, loser);
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
            User bob,
            User loser) {
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
}
