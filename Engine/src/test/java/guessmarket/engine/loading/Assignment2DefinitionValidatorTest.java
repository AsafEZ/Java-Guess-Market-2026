package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Assignment2DefinitionValidatorTest {
    private final Assignment2DefinitionValidator validator =
            new Assignment2DefinitionValidator();

    @Test
    void validLmsrAndOrderBookDefinitionPasses() {
        assertDoesNotThrow(() -> validator.validate(validDefinition()));
    }

    @Test
    void duplicateTrimmedUserNameIsRejectedCaseSensitively() {
        Assignment2Definition source = validDefinition();
        Assignment2Definition duplicate = new Assignment2Definition(
                List.of(
                        source.users().getFirst(),
                        new UserDefinition(" Alice ", 50, List.of(2))),
                source.events());
        assertError(ErrorCode.DUPLICATE_USER_NAME, duplicate);

        Assignment2Definition caseVariant = new Assignment2Definition(
                List.of(
                        source.users().getFirst(),
                        new UserDefinition("alice", 50, List.of(2))),
                source.events());
        assertDoesNotThrow(() -> validator.validate(caseVariant));
    }

    @Test
    void blankUserNameAndNonPositiveInitialCashAreRejected() {
        Assignment2Definition source = validDefinition();
        assertError(
                ErrorCode.INVALID_USER_NAME,
                withUsers(source, List.of(
                        new UserDefinition("   ", 100, List.of(1)),
                        source.users().get(1))));
        assertError(
                ErrorCode.INVALID_INITIAL_CASH,
                withUsers(source, List.of(
                        new UserDefinition("Alice", 0, List.of(1)),
                        source.users().get(1))));
    }

    @Test
    void duplicateEventIdIsRejectedAcrossFullIntegerRange() {
        Assignment2Definition source = validDefinition();
        Assignment2EventDefinition first = source.events().getFirst();
        Assignment2EventDefinition duplicate = event(
                first.id(),
                "Duplicate",
                new OrderBookDefinition(true, 0, 1));
        assertError(
                ErrorCode.DUPLICATE_EVENT_ID,
                new Assignment2Definition(
                        source.users(), List.of(first, duplicate)));
    }

    @Test
    void eventTextAndExactlyTwoDistinctOptionNamesAreRequired() {
        Assignment2Definition source = validDefinition();
        Assignment2EventDefinition base = source.events().getFirst();
        assertError(
                ErrorCode.INVALID_EVENT_DEFINITION,
                withFirstEvent(source, new Assignment2EventDefinition(
                        base.id(), " ", base.description(), base.commission(),
                        base.options(), base.tradingMechanism())));
        assertError(
                ErrorCode.INVALID_OPTION_COUNT,
                withFirstEvent(source, new Assignment2EventDefinition(
                        base.id(), base.name(), base.description(), base.commission(),
                        List.of(new OptionDefinition("Yes")),
                        base.tradingMechanism())));
        assertError(
                ErrorCode.INVALID_EVENT_DEFINITION,
                withFirstEvent(source, new Assignment2EventDefinition(
                        base.id(), base.name(), base.description(), base.commission(),
                        List.of(
                                new OptionDefinition("Yes"),
                                new OptionDefinition(" Yes ")),
                        base.tradingMechanism())));
    }

    @Test
    void commissionMustBeBetweenZeroAndNinetyInclusive() {
        Assignment2Definition source = validDefinition();
        Assignment2EventDefinition base = source.events().getFirst();
        assertError(
                ErrorCode.INVALID_COMMISSION,
                withFirstEvent(source, withCommission(base, -1)));
        assertError(
                ErrorCode.INVALID_COMMISSION,
                withFirstEvent(source, withCommission(base, 91)));
        assertDoesNotThrow(() -> validator.validate(
                withFirstEvent(source, withCommission(base, 90))));
    }

    @Test
    void lmsrBMustBePositive() {
        Assignment2Definition source = validDefinition();
        assertError(
                ErrorCode.INVALID_LMSR_B,
                withFirstEvent(source, event(1, "LMSR", new LmsrDefinition(0))));
    }

    @Test
    void orderBookInitialMayBeZeroButNotNegativeAndDMustBePositive() {
        Assignment2Definition source = validDefinition();
        assertDoesNotThrow(() -> validator.validate(source));
        assertError(
                ErrorCode.INVALID_ORDER_BOOK_INITIAL,
                withSecondEvent(source, event(
                        2, "Book", new OrderBookDefinition(true, -1, 1))));
        assertError(
                ErrorCode.INVALID_ORDER_BOOK_D,
                withSecondEvent(source, event(
                        2, "Book", new OrderBookDefinition(true, 0, 0))));
    }

    @Test
    void marketMakerReferenceMustResolve() {
        Assignment2Definition source = validDefinition();
        assertError(
                ErrorCode.EVENT_NOT_FOUND,
                withUsers(source, List.of(
                        new UserDefinition("Alice", 100, List.of(99)),
                        source.users().get(1))));
    }

    @Test
    void everyEventRequiresExactlyOneMarketMaker() {
        Assignment2Definition source = validDefinition();
        assertError(
                ErrorCode.MARKET_MAKER_NOT_ASSIGNED,
                withUsers(source, List.of(
                        new UserDefinition("Alice", 100, List.of(1)),
                        new UserDefinition("Bob", 100, List.of()))));
        assertError(
                ErrorCode.MARKET_MAKER_ALREADY_ASSIGNED,
                withUsers(source, List.of(
                        new UserDefinition("Alice", 100, List.of(1, 2)),
                        new UserDefinition("Bob", 100, List.of(2)))));
    }

    private void assertError(ErrorCode expected, Assignment2Definition definition) {
        EngineException exception = assertThrows(
                EngineException.class,
                () -> validator.validate(definition));
        assertEquals(expected, exception.getErrorCode());
    }

    private static Assignment2Definition validDefinition() {
        return new Assignment2Definition(
                List.of(
                        new UserDefinition("Alice", 100, List.of(1)),
                        new UserDefinition("Bob", 100, List.of(2))),
                List.of(
                        event(1, "LMSR", new LmsrDefinition(10)),
                        event(2, "Book", new OrderBookDefinition(true, 0, 1))));
    }

    private static Assignment2EventDefinition event(
            int id,
            String name,
            TradingMechanismDefinition mechanism) {
        return new Assignment2EventDefinition(
                id,
                name,
                "Description",
                new CommissionDefinition(CommissionType.ON_PURCHASE, 5),
                List.of(new OptionDefinition("Yes"), new OptionDefinition("No")),
                mechanism);
    }

    private static Assignment2EventDefinition withCommission(
            Assignment2EventDefinition source,
            int percentage) {
        return new Assignment2EventDefinition(
                source.id(), source.name(), source.description(),
                new CommissionDefinition(source.commission().type(), percentage),
                source.options(), source.tradingMechanism());
    }

    private static Assignment2Definition withUsers(
            Assignment2Definition source,
            List<UserDefinition> users) {
        return new Assignment2Definition(users, source.events());
    }

    private static Assignment2Definition withFirstEvent(
            Assignment2Definition source,
            Assignment2EventDefinition event) {
        return new Assignment2Definition(
                source.users(), List.of(event, source.events().get(1)));
    }

    private static Assignment2Definition withSecondEvent(
            Assignment2Definition source,
            Assignment2EventDefinition event) {
        return new Assignment2Definition(
                source.users(), List.of(source.events().getFirst(), event));
    }
}
