package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v2.GMEvent;
import guessmarket.engine.loading.jaxb.v2.GMLMSR;
import guessmarket.engine.loading.jaxb.v2.GMOrderBook;
import guessmarket.engine.loading.jaxb.v2.GMUser;
import guessmarket.engine.loading.jaxb.v2.GuessMarket;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Assignment2JaxbDefinitionMapperTest {
    private static final String SMALL = "assignment2/xml/valid/small.xml";
    private static final String MULTIPLE = "assignment2/xml/valid/multiple.xml";
    private static final String BOUNDARIES =
            "assignment2/xml/edge-cases/schema-boundaries.xml";
    private static final String ERROR_2 =
            "assignment2/xml/invalid-business-rules/error-2.xml";
    private static final String ERROR_3 =
            "assignment2/xml/invalid-business-rules/error-3.xml";

    private final Assignment2XmlUnmarshaller unmarshaller =
            new Assignment2XmlUnmarshaller();
    private final Assignment2JaxbDefinitionMapper mapper =
            new Assignment2JaxbDefinitionMapper();

    @Test
    void smallFixtureMapsEveryDefinitionField() throws Exception {
        Assignment2Definition definition = map(SMALL);

        assertEquals(
                List.of(
                        new UserDefinition("Avrum", 1000, List.of(2)),
                        new UserDefinition("Tikva", 10000, List.of(1)),
                        new UserDefinition("Menash", 100, List.of())),
                definition.users());
        assertEquals(2, definition.events().size());

        Assignment2EventDefinition lmsr = definition.events().get(0);
        assertEquals(1, lmsr.id());
        assertEquals("Mujtaba is Dead", lmsr.name());
        assertEquals(
                "This event gambles if Mujtaba is a live or not. it will be "
                        + "determined if he will be shown in public until 31.8.26",
                lmsr.description());
        assertEquals(
                new CommissionDefinition(CommissionType.ON_PURCHASE, 5),
                lmsr.commission());
        assertEquals(
                List.of(
                        new OptionDefinition("Hell Yea !"),
                        new OptionDefinition("No way !")),
                lmsr.options());
        assertEquals(new LmsrDefinition(100), lmsr.tradingMechanism());

        Assignment2EventDefinition orderBook = definition.events().get(1);
        assertEquals(2, orderBook.id());
        assertEquals("World Cap Winner", orderBook.name());
        assertEquals(
                "Who do you think will win the world cap ?",
                orderBook.description());
        assertEquals(
                new CommissionDefinition(CommissionType.ON_CLOSE, 15),
                orderBook.commission());
        assertEquals(
                List.of(
                        new OptionDefinition("Argentina"),
                        new OptionDefinition("Spain")),
                orderBook.options());
        assertEquals(
                new OrderBookDefinition(true, 100, 1),
                orderBook.tradingMechanism());
    }

    @Test
    void multipleFixturePreservesUserEventOptionAndAssignmentOrder()
            throws Exception {
        Assignment2Definition definition = map(MULTIPLE);

        assertEquals(
                List.of("Avrum", "Tikva", "Menash"),
                definition.users().stream().map(UserDefinition::name).toList());
        assertEquals(
                List.of(1000, 10000, 100),
                definition.users().stream()
                        .map(UserDefinition::initialCash)
                        .toList());
        assertEquals(
                List.of(2),
                definition.users().get(0).marketMakerEventIds());
        assertEquals(
                List.of(1, 4, 3),
                definition.users().get(1).marketMakerEventIds());
        assertEquals(
                List.of(
                        "Mujtaba is Dead",
                        "World Cap Winner",
                        "Earth Quake on Dead Sea",
                        "Will it rain tomorrow ?"),
                definition.events().stream()
                        .map(Assignment2EventDefinition::name)
                        .toList());
        assertEquals(
                List.of(1, 2, 3, 4),
                definition.events().stream()
                        .map(Assignment2EventDefinition::id)
                        .toList());
        assertEquals(
                List.of(
                        new CommissionDefinition(CommissionType.ON_PURCHASE, 5),
                        new CommissionDefinition(CommissionType.ON_CLOSE, 10),
                        new CommissionDefinition(CommissionType.ON_PURCHASE, 50),
                        new CommissionDefinition(CommissionType.ON_CLOSE, 10)),
                definition.events().stream()
                        .map(Assignment2EventDefinition::commission)
                        .toList());
        assertEquals(
                List.of(new OptionDefinition("Yes"), new OptionDefinition("No")),
                definition.events().get(2).options());
        assertEquals(
                new OrderBookDefinition(false, 1000, 1),
                definition.events().get(2).tradingMechanism());
        assertEquals(
                new LmsrDefinition(200),
                definition.events().get(3).tradingMechanism());
    }

    @Test
    void mappingDoesNotMutateTheJaxbGraph() throws Exception {
        GuessMarket source = unmarshal(SMALL);
        List<GMUser> sourceUsers = source.getGMUsers().getGMUser();
        List<GMEvent> sourceEvents = source.getGMEvents().getGMEvent();
        List<String> sourceOptions = sourceEvents.getFirst()
                .getGMOptions().getGMOption();
        List<guessmarket.engine.loading.jaxb.v2.Event> sourceAssignments =
                sourceUsers.getFirst().getGMMarketMaker().getEvent();

        mapper.map(source);

        assertSame(sourceUsers, source.getGMUsers().getGMUser());
        assertSame(sourceEvents, source.getGMEvents().getGMEvent());
        assertSame(
                sourceOptions,
                sourceEvents.getFirst().getGMOptions().getGMOption());
        assertSame(
                sourceAssignments,
                sourceUsers.getFirst().getGMMarketMaker().getEvent());
        assertEquals(List.of("Avrum", "Tikva", "Menash"),
                sourceUsers.stream().map(GMUser::getName).toList());
        assertEquals(
                List.of("Hell Yea !", "No way !"),
                sourceOptions);
        assertEquals(
                List.of(2),
                sourceAssignments.stream()
                        .map(guessmarket.engine.loading.jaxb.v2.Event::getId)
                        .toList());
    }

    @Test
    void schemaBoundaryValuesAreMappedWithoutBusinessValidation()
            throws Exception {
        Assignment2Definition definition = map(BOUNDARIES);

        assertEquals(0, definition.users().getFirst().initialCash());
        assertEquals(
                List.of(0, -1),
                definition.users().getFirst().marketMakerEventIds());
        assertEquals(
                List.of(0, -1),
                definition.events().stream()
                        .map(Assignment2EventDefinition::id)
                        .toList());
        assertEquals(
                List.of(new OptionDefinition("Only option")),
                definition.events().getFirst().options());
        assertEquals(
                new CommissionDefinition(CommissionType.ON_PURCHASE, 0),
                definition.events().getFirst().commission());
        assertEquals(
                new LmsrDefinition(0),
                definition.events().getFirst().tradingMechanism());
        assertEquals(
                new OrderBookDefinition(false, -1, 0),
                definition.events().get(1).tradingMechanism());
    }

    @Test
    void fullIntegerEventIdentifiersArePreserved() throws Exception {
        GuessMarket source = unmarshal(BOUNDARIES);
        source.getGMEvents().getGMEvent().get(0).setId(Integer.MIN_VALUE);
        source.getGMEvents().getGMEvent().get(1).setId(Integer.MAX_VALUE);
        source.getGMUsers().getGMUser().getFirst()
                .getGMMarketMaker().getEvent().get(0).setId(Integer.MIN_VALUE);
        source.getGMUsers().getGMUser().getFirst()
                .getGMMarketMaker().getEvent().get(1).setId(Integer.MAX_VALUE);

        Assignment2Definition definition = mapper.map(source);

        assertEquals(
                List.of(Integer.MIN_VALUE, Integer.MAX_VALUE),
                definition.events().stream()
                        .map(Assignment2EventDefinition::id)
                        .toList());
        assertEquals(
                List.of(Integer.MIN_VALUE, Integer.MAX_VALUE),
                definition.users().getFirst().marketMakerEventIds());
    }

    @Test
    void allDefinitionCollectionsAreImmutableSnapshots() throws Exception {
        Assignment2Definition definition = map(SMALL);

        assertThrows(
                UnsupportedOperationException.class,
                () -> definition.users().add(
                        new UserDefinition("Extra", 1, List.of())));
        assertThrows(
                UnsupportedOperationException.class,
                () -> definition.events().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> definition.users().getFirst()
                        .marketMakerEventIds().add(3));
        assertThrows(
                UnsupportedOperationException.class,
                () -> definition.events().getFirst().options().clear());
    }

    @Test
    void laterJaxbMutationDoesNotChangeMappedDefinition() throws Exception {
        GuessMarket source = unmarshal(SMALL);
        GMUser firstUser = source.getGMUsers().getGMUser().getFirst();
        GMEvent firstEvent = source.getGMEvents().getGMEvent().getFirst();
        Assignment2Definition definition = mapper.map(source);

        firstUser.setName("Changed user");
        firstUser.getGMMarketMaker().getEvent().clear();
        firstEvent.setName("Changed event");
        firstEvent.getGMOptions().getGMOption().set(0, "Changed option");
        source.getGMUsers().getGMUser().clear();
        source.getGMEvents().getGMEvent().clear();

        assertEquals(3, definition.users().size());
        assertEquals("Avrum", definition.users().getFirst().name());
        assertEquals(List.of(2), definition.users().getFirst().marketMakerEventIds());
        assertEquals(2, definition.events().size());
        assertEquals("Mujtaba is Dead", definition.events().getFirst().name());
        assertEquals(
                "Hell Yea !",
                definition.events().getFirst().options().getFirst().name());
    }

    @Test
    void sameMapperCanMapRepeatedCallsWithoutSharedState() throws Exception {
        Assignment2Definition first = map(SMALL);
        Assignment2Definition second = map(MULTIPLE);
        Assignment2Definition third = map(SMALL);

        assertEquals(2, first.events().size());
        assertEquals(4, second.events().size());
        assertEquals(first, third);
    }

    @Test
    void businessInvalidFixturesStillMapStructurally() throws Exception {
        Assignment2Definition invalidCash = map(ERROR_2);
        Assignment2Definition invalidReference = map(ERROR_3);

        assertEquals(0, invalidCash.users().getFirst().initialCash());
        assertTrue(invalidReference.users().stream()
                .flatMap(user -> user.marketMakerEventIds().stream())
                .anyMatch(eventId -> eventId == 12));
        assertFalse(invalidReference.events().stream()
                .anyMatch(event -> event.id() == 12));
    }

    @Test
    void textValuesAreMappedWithoutTrimmingOrCanonicalization()
            throws Exception {
        GuessMarket source = unmarshal(SMALL);
        source.getGMUsers().getGMUser().getFirst().setName("  Alice  ");
        source.getGMEvents().getGMEvent().getFirst().setName("  Event  ");
        source.getGMEvents().getGMEvent().getFirst()
                .getGMOptions().getGMOption().set(0, "  Yes  ");

        Assignment2Definition definition = mapper.map(source);

        assertEquals("  Alice  ", definition.users().getFirst().name());
        assertEquals("  Event  ", definition.events().getFirst().name());
        assertEquals(
                "  Yes  ",
                definition.events().getFirst().options().getFirst().name());
    }

    @Test
    void eachTradingMechanismChoiceMapsToItsOwnDefinitionType()
            throws Exception {
        Assignment2Definition definition = map(SMALL);

        assertInstanceOf(
                LmsrDefinition.class,
                definition.events().getFirst().tradingMechanism());
        assertInstanceOf(
                OrderBookDefinition.class,
                definition.events().get(1).tradingMechanism());
    }

    @Test
    void missingOrMultipleTradingMechanismsAreRejectedConsistently()
            throws Exception {
        GuessMarket missing = unmarshal(SMALL);
        missing.getGMEvents().getGMEvent().getFirst().getGMMethod().setGMLMSR(null);

        EngineException missingException = assertThrows(
                EngineException.class,
                () -> mapper.map(missing));
        assertEquals(ErrorCode.XML_PARSE_ERROR, missingException.getErrorCode());

        GuessMarket multiple = unmarshal(SMALL);
        GMLMSR lmsr = new GMLMSR();
        lmsr.setB(5);
        multiple.getGMEvents().getGMEvent().get(1).getGMMethod().setGMLMSR(lmsr);

        EngineException multipleException = assertThrows(
                EngineException.class,
                () -> mapper.map(multiple));
        assertEquals(ErrorCode.XML_PARSE_ERROR, multipleException.getErrorCode());
    }

    @Test
    void unexpectedSchemaEnumerationsAreNotSilentlyNormalized()
            throws Exception {
        GuessMarket commissionSource = unmarshal(SMALL);
        commissionSource.getGMEvents().getGMEvent().getFirst()
                .getCommission().setType("ON-PURCHASE");

        EngineException commissionException = assertThrows(
                EngineException.class,
                () -> mapper.map(commissionSource));
        assertEquals(ErrorCode.XML_PARSE_ERROR, commissionException.getErrorCode());

        GuessMarket orderBookSource = unmarshal(SMALL);
        GMOrderBook orderBook = orderBookSource.getGMEvents()
                .getGMEvent().get(1).getGMMethod().getGMOrderBook();
        orderBook.setAllowMint("TRUE");

        EngineException orderBookException = assertThrows(
                EngineException.class,
                () -> mapper.map(orderBookSource));
        assertEquals(ErrorCode.XML_PARSE_ERROR, orderBookException.getErrorCode());
    }

    private Assignment2Definition map(String resource) throws Exception {
        return mapper.map(unmarshal(resource));
    }

    private GuessMarket unmarshal(String resource) throws Exception {
        return unmarshaller.unmarshal(resourcePath(resource));
    }

    private static Path resourcePath(String resourcePath)
            throws URISyntaxException {
        URL resource = Assignment2JaxbDefinitionMapperTest.class
                .getClassLoader()
                .getResource(resourcePath);
        if (resource == null) {
            throw new AssertionError("Missing classpath resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }
}
