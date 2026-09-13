package guessmarket.engine.impl;

import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.PurchaseOutcome;
import guessmarket.engine.domain.SettlementOutcome;
import guessmarket.engine.domain.User;
import guessmarket.engine.domain.CommissionPolicy;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.dto.LmsrEventDetails;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.OptionPositionDetails;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Task2DtoMapperTest {

    @Test
    void mapsLmsrEventSummaryAndDetailsWithoutConcreteMechanismAccess() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        PurchaseOutcome purchase = fixture.system.purchaseShares("Buyer", 1, 1, 3L);

        MarketEventSummary summary = MarketEventDtoMapper.toSummary(fixture.event);
        MarketEventDetails details = MarketEventDtoMapper.toDetails(
                fixture.event, fixture.system);
        LmsrEventDetails lmsr = assertInstanceOf(
                LmsrEventDetails.class, details.mechanismDetails());

        assertEquals(1, summary.eventId());
        assertEquals("Event 1", summary.name());
        assertEquals(EventStatus.ACTIVE, summary.status());
        assertEquals(TradingMethod.LMSR, summary.tradingMethod());
        assertEquals(10, summary.commissionPercentage());
        assertEquals(CommissionType.ON_PURCHASE, summary.commissionType());
        assertEquals(Optional.of("Maker"), summary.marketMakerName());
        assertEquals(2, summary.options().size());

        assertEquals(summary.eventId(), details.eventId());
        assertEquals(summary.accountBalance(), details.accountBalance());
        assertEquals(Optional.empty(), details.winningOptionNumber());
        assertEquals(Optional.empty(), details.winningOptionName());
        assertEquals(1, details.participantPositions().size());
        assertEquals("Buyer", details.participantPositions().getFirst().userName());
        assertEquals(TradingMethod.LMSR, lmsr.tradingMethod());
        assertEquals(10, lmsr.b());
        assertEquals(2, lmsr.options().size());
        assertEquals(3L, lmsr.options().getFirst().purchasedShares());
        assertTrue(Double.isFinite(lmsr.options().getFirst().currentValue()));
        assertEquals(1, lmsr.tradesNewestFirst().size());
        assertEquals(purchase.shareCost(), lmsr.tradesNewestFirst().getFirst().shareCost());
    }

    @Test
    void mapsUserPositionsWithShareCostAndCommissionKeptSeparate() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        PurchaseOutcome purchase = fixture.system.purchaseShares("Buyer", 1, 1, 4L);

        UserSummary summary = UserDtoMapper.toSummary(fixture.buyer);
        UserDetails details = UserDtoMapper.toDetails(fixture.buyer, fixture.system);
        PositionDetails position = details.positions().getFirst();
        OptionPositionDetails firstOption = position.options().getFirst();

        assertEquals("Buyer", summary.name());
        assertEquals(fixture.buyer.getBalance(), summary.balance());
        assertEquals(UserStatus.ACTIVE, summary.status());
        assertEquals(Set.of(), details.marketMakerEventIds());
        assertEquals(1, details.positions().size());
        assertEquals("Buyer", position.userName());
        assertEquals(1, position.eventId());
        assertFalse(position.marketMaker());
        assertEquals(4L, firstOption.shares());
        assertEquals(purchase.shareCost(), firstOption.amountPaid());
        assertEquals(purchase.commission(), firstOption.commissionPaid());
        assertEquals(purchase.shareCost(), position.totalAmountPaid());
        assertEquals(purchase.commission(), position.totalCommissionPaid());
        assertEquals(1, position.tradesNewestFirst().size());

        UserDetails makerDetails = UserDtoMapper.toDetails(
                fixture.marketMaker, fixture.system);
        assertEquals(Set.of(1), makerDetails.marketMakerEventIds());
    }

    @Test
    void purchaseMapperAddsBuyerAndUpdatedPublicSnapshots() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        PurchaseOutcome outcome = fixture.system.purchaseShares(" Buyer ", 1, 2, 2L);

        UserPurchaseResult result = PurchaseDtoMapper.toResult(
                " Buyer ", 1, outcome, fixture.system);

        assertEquals("Buyer", result.buyerName());
        assertEquals(1, result.eventId());
        assertEquals(outcome.optionNumber(), result.optionNumber());
        assertEquals(outcome.shareQuantity(), result.shareQuantity());
        assertEquals(outcome.shareCost(), result.shareCost());
        assertEquals(outcome.commission(), result.commission());
        assertEquals(outcome.totalPaid(), result.totalPaid());
        assertEquals(2L, result.updatedBuyer().positions().getFirst().totalShares());
        assertEquals(2L, result.updatedEvent().participantPositions()
                .getFirst().totalShares());
    }

    @Test
    void settlementMapperPreservesConsolidatedMarketMakerWinnerCredit() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_CLOSE);
        fixture.system.purchaseShares("Maker", 1, 1, 2L);
        fixture.system.purchaseShares("Buyer", 1, 1, 3L);
        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);

        SettlementResult result = SettlementDtoMapper.toResult(outcome, fixture.system);
        long makerCredits = result.accountCredits().stream()
                .filter(credit -> credit.userName().equals("Maker"))
                .count();

        assertEquals(outcome.eventId(), result.eventId());
        assertEquals(outcome.winningOptionNumber(), result.winningOptionNumber());
        assertEquals("Yes", result.winningOptionName());
        assertEquals(outcome.marketMakerName(), result.marketMakerName());
        assertEquals(outcome.userSettlements().size(), result.userSettlements().size());
        assertEquals(outcome.accountCredits().size(), result.accountCredits().size());
        assertEquals(1L, makerCredits);
        assertEquals(outcome.totalGrossPayout(), result.totalGrossPayout());
        assertEquals(outcome.totalClosingCommission(), result.totalClosingCommission());
        assertEquals(outcome.totalWinnerNetPayout(), result.totalWinnerNetPayout());
        assertEquals(outcome.marketMakerResidual(), result.marketMakerResidual());
        assertEquals(outcome.totalMarketMakerCredit(), result.totalMarketMakerCredit());
        assertEquals(outcome.eventBalanceBefore(), result.eventBalanceBefore());
        assertEquals(0.0, result.eventBalanceAfter());
        assertEquals(EventStatus.CLOSED, result.closedEvent().status());
        assertEquals(Optional.of(1), result.closedEvent().winningOptionNumber());
    }

    @Test
    void mappedCollectionsCannotModifyDtoState() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_CLOSE);
        fixture.system.purchaseShares("Maker", 1, 1, 1L);
        SettlementOutcome outcome = fixture.system.closeEvent(1, "Maker", 1);
        MarketEventDetails event = MarketEventDtoMapper.toDetails(
                fixture.event, fixture.system);
        UserDetails user = UserDtoMapper.toDetails(fixture.marketMaker, fixture.system);
        PositionDetails position = user.positions().getFirst();
        LmsrEventDetails lmsr = assertInstanceOf(
                LmsrEventDetails.class, event.mechanismDetails());
        SettlementResult settlement = SettlementDtoMapper.toResult(
                outcome, fixture.system);

        assertThrows(UnsupportedOperationException.class, () -> event.options().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> event.participantPositions().clear());
        assertThrows(UnsupportedOperationException.class, () -> lmsr.options().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> lmsr.tradesNewestFirst().clear());
        assertThrows(UnsupportedOperationException.class, () -> user.positions().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> user.marketMakerEventIds().add(2));
        assertThrows(UnsupportedOperationException.class, () -> position.options().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> position.tradesNewestFirst().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> settlement.userSettlements().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> settlement.accountCredits().clear());
    }

    @Test
    void mappedSnapshotsDoNotChangeAfterFurtherDomainMutation() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        PurchaseOutcome firstPurchase = fixture.system.purchaseShares("Buyer", 1, 1, 1L);
        UserPurchaseResult snapshot = PurchaseDtoMapper.toResult(
                "Buyer", 1, firstPurchase, fixture.system);
        LmsrEventDetails eventSnapshot = assertInstanceOf(
                LmsrEventDetails.class, snapshot.updatedEvent().mechanismDetails());

        fixture.system.purchaseShares("Buyer", 1, 1, 2L);

        assertEquals(1L, eventSnapshot.options().getFirst().purchasedShares());
        assertEquals(1, eventSnapshot.tradesNewestFirst().size());
        assertEquals(1L, snapshot.updatedBuyer().positions().getFirst().totalShares());
        assertEquals(
                firstPurchase.shareCost(),
                snapshot.updatedBuyer().positions().getFirst().totalAmountPaid());
    }

    @Test
    void userTradeHistoryContainsOnlyThatUsersTask2Trades() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        fixture.system.purchaseShares("Buyer", 1, 1, 1L);
        fixture.system.purchaseShares("Maker", 1, 2, 2L);

        UserDetails buyer = UserDtoMapper.toDetails(fixture.buyer, fixture.system);
        UserDetails maker = UserDtoMapper.toDetails(fixture.marketMaker, fixture.system);

        assertEquals(1, buyer.positions().getFirst().tradesNewestFirst().size());
        assertEquals(1, maker.positions().getFirst().tradesNewestFirst().size());
        assertEquals(1, buyer.positions().getFirst().tradesNewestFirst()
                .getFirst().optionNumber());
        assertEquals(2, maker.positions().getFirst().tradesNewestFirst()
                .getFirst().optionNumber());
    }

    @Test
    void dtoConstructorsCopyCallerOwnedCollections() {
        Fixture fixture = createOpenedFixture(CommissionType.ON_PURCHASE);
        fixture.system.purchaseShares("Buyer", 1, 1, 1L);
        PositionDetails mappedPosition = UserDtoMapper.toDetails(
                fixture.buyer, fixture.system).positions().getFirst();
        List<PositionDetails> positions = new ArrayList<>(List.of(mappedPosition));
        Set<Integer> makerEvents = new HashSet<>(Set.of(1));

        UserDetails details = new UserDetails(
                "Buyer", 10.0, UserStatus.ACTIVE, makerEvents, positions);
        positions.clear();
        makerEvents.clear();

        assertEquals(1, details.positions().size());
        assertEquals(Set.of(1), details.marketMakerEventIds());
    }

    private static Fixture createOpenedFixture(CommissionType commissionType) {
        MarketSystem system = new MarketSystem();
        User buyer = new User("Buyer", 1_000.0);
        User marketMaker = new User("Maker", 1_000.0);
        MarketEvent event = MarketEvent.createNotStartedEvent(
                1,
                "Event 1",
                "Description",
                List.of(new MarketOption(1, "Yes"), new MarketOption(2, "No")),
                new CommissionPolicy(10, commissionType),
                new LmsrTradingMechanism(10, new LmsrCalculator()));
        system.addUser(buyer);
        system.addUser(marketMaker);
        system.addEvent(event);
        system.assignMarketMaker(1, "Maker");
        system.openEvent(1, "Maker");
        return new Fixture(system, event, buyer, marketMaker);
    }

    private record Fixture(
            MarketSystem system,
            MarketEvent event,
            User buyer,
            User marketMaker) {
    }
}
