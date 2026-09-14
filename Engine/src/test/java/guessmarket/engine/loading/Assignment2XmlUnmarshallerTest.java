package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v2.GMEvent;
import guessmarket.engine.loading.jaxb.v2.GMOrderBook;
import guessmarket.engine.loading.jaxb.v2.GMUser;
import guessmarket.engine.loading.jaxb.v2.GuessMarket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Assignment2XmlUnmarshallerTest {
    private static final String SMALL = "assignment2/xml/valid/small.xml";
    private static final String MULTIPLE = "assignment2/xml/valid/multiple.xml";
    private static final String BOUNDARIES =
            "assignment2/xml/edge-cases/schema-boundaries.xml";

    private final Assignment2XmlUnmarshaller unmarshaller =
            new Assignment2XmlUnmarshaller();

    @Test
    void smallFixtureUnmarshalsUsersEventsAndMechanisms() throws Exception {
        GuessMarket model = unmarshaller.unmarshal(resourcePath(SMALL));

        List<GMUser> users = model.getGMUsers().getGMUser();
        List<GMEvent> events = model.getGMEvents().getGMEvent();
        assertEquals(List.of("Avrum", "Tikva", "Menash"), userNames(users));
        assertEquals(List.of(1000, 10000, 100), initialCash(users));
        assertEquals(List.of(2), marketMakerEventIds(users.get(0)));
        assertEquals(List.of(1), marketMakerEventIds(users.get(1)));
        assertNull(users.get(2).getGMMarketMaker());
        assertEquals(2, events.size());

        GMEvent lmsr = events.get(0);
        assertEquals(1, lmsr.getId());
        assertEquals("Mujtaba is Dead", lmsr.getName());
        assertEquals(5, lmsr.getCommission().getValue());
        assertEquals("on-purchase", lmsr.getCommission().getType());
        assertEquals(List.of("Hell Yea !", "No way !"), lmsr.getGMOptions().getGMOption());
        assertEquals(100, lmsr.getGMMethod().getGMLMSR().getB());
        assertNull(lmsr.getGMMethod().getGMOrderBook());

        GMEvent orderBookEvent = events.get(1);
        assertEquals(2, orderBookEvent.getId());
        assertEquals("on-close", orderBookEvent.getCommission().getType());
        assertNull(orderBookEvent.getGMMethod().getGMLMSR());
        GMOrderBook orderBook = orderBookEvent.getGMMethod().getGMOrderBook();
        assertEquals("true", orderBook.getAllowMint());
        assertEquals(100, orderBook.getInitial());
        assertEquals(1, orderBook.getD());
    }

    @Test
    void multipleFixtureUnmarshalsAllRecords() throws Exception {
        GuessMarket model = unmarshaller.unmarshal(resourcePath(MULTIPLE));

        assertEquals(4, model.getGMEvents().getGMEvent().size());
        assertEquals(3, model.getGMUsers().getGMUser().size());
        assertEquals(
                List.of(1, 4, 3),
                marketMakerEventIds(model.getGMUsers().getGMUser().get(1)));
    }

    @Test
    void schemaBoundariesRemainJaxbValuesRatherThanBusinessFailures()
            throws Exception {
        GuessMarket model = unmarshaller.unmarshal(resourcePath(BOUNDARIES));

        GMUser user = model.getGMUsers().getGMUser().getFirst();
        List<GMEvent> events = model.getGMEvents().getGMEvent();
        assertEquals(0, user.getInitialCash());
        assertEquals(List.of(0, -1), marketMakerEventIds(user));
        assertEquals(List.of(0, -1), events.stream().map(GMEvent::getId).toList());
        assertEquals(List.of("Only option"), events.get(0).getGMOptions().getGMOption());
        assertEquals(0, events.get(0).getGMMethod().getGMLMSR().getB());
        assertEquals(-1, events.get(1).getGMMethod().getGMOrderBook().getInitial());
        assertEquals(0, events.get(1).getGMMethod().getGMOrderBook().getD());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "missing-users.xml",
            "missing-events.xml",
            "misspelled-commission.xml",
            "multiple-trading-methods.xml",
            "missing-order-book-attribute.xml",
            "invalid-allow-mint.xml",
            "too-many-options.xml",
            "invalid-event-sequence.xml",
            "non-integer-event-id.xml"
    })
    void schemaInvalidFixturesAreRejected(String fixtureName) throws Exception {
        Path xml = resourcePath("assignment2/xml/invalid-schema/" + fixtureName);

        assertXmlParseError(xml);
    }

    @Test
    void malformedXmlIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "malformed.xml",
                "<Guess-Market><GM-events></Guess-Market>");

        assertXmlParseError(xml);
    }

    @Test
    void doctypeIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "doctype.xml",
                """
                        <?xml version="1.0"?>
                        <!DOCTYPE Guess-Market [<!ELEMENT Guess-Market ANY>]>
                        <Guess-Market><GM-events/><GM-users/></Guess-Market>
                        """);

        EngineException exception = assertXmlParseError(xml);
        assertTrue(
                exception.getMessage().contains("DOCTYPE")
                        || exception.getCause() != null);
    }

    @Test
    void externalEntityIsRejectedWithoutReadingItsTarget(@TempDir Path tempDir)
            throws Exception {
        Path external = tempDir.resolve("must-not-be-read.txt");
        Files.writeString(external, "EXTERNAL_SECRET_MARKER");
        Path xml = writeXml(
                tempDir,
                "xxe.xml",
                """
                        <?xml version="1.0"?>
                        <!DOCTYPE Guess-Market [<!ENTITY xxe SYSTEM "%s">]>
                        <Guess-Market><GM-events><value>&xxe;</value></GM-events><GM-users/></Guess-Market>
                        """.formatted(external.toUri()));

        EngineException exception = assertXmlParseError(xml);
        assertTrue(!exception.getMessage().contains("EXTERNAL_SECRET_MARKER"));
    }

    @Test
    void externalSchemaHintIsNotLoaded(@TempDir Path tempDir) throws Exception {
        String source = Files.readString(resourcePath(SMALL));
        Path xml = writeXml(
                tempDir,
                "external-schema-hint.xml",
                source.replace(
                        "GM-EX2-Schema.xsd",
                        "http://127.0.0.1:9/must-not-be-loaded.xsd"));

        GuessMarket model = unmarshaller.unmarshal(xml);
        assertEquals(2, model.getGMEvents().getGMEvent().size());
    }

    @Test
    void assignmentOneDocumentIsRejectedWithoutFallback(@TempDir Path tempDir)
            throws Exception {
        Path xml = writeXml(
                tempDir,
                "assignment-one.xml",
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Guess-Market>
                            <GM-events>
                                <GM-event name="Legacy">
                                    <id>1</id>
                                    <description>Legacy event</description>
                                    <comision type="on-purchase">5</comision>
                                    <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                                    <GM-method><GM-LMSR><b>10</b></GM-LMSR></GM-method>
                                </GM-event>
                            </GM-events>
                        </Guess-Market>
                        """);

        assertXmlParseError(xml);
    }

    @Test
    void missingPathUsesFileNotFoundError(@TempDir Path tempDir) {
        EngineException exception = assertThrows(
                EngineException.class,
                () -> unmarshaller.unmarshal(tempDir.resolve("missing.xml")));

        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void repeatedUnmarshalCallsDoNotShareMutableState() throws Exception {
        GuessMarket first = unmarshaller.unmarshal(resourcePath(SMALL));
        GuessMarket second = unmarshaller.unmarshal(resourcePath(MULTIPLE));
        GuessMarket third = unmarshaller.unmarshal(resourcePath(SMALL));

        assertEquals(2, first.getGMEvents().getGMEvent().size());
        assertEquals(4, second.getGMEvents().getGMEvent().size());
        assertEquals(2, third.getGMEvents().getGMEvent().size());
        assertNotEquals(first, third);
    }

    @Test
    void assignmentOneAndAssignmentTwoBindingsCoexist() {
        Object assignmentOne = new guessmarket.engine.loading.jaxb.GuessMarket();
        Object assignmentTwo = new GuessMarket();

        assertInstanceOf(
                guessmarket.engine.loading.jaxb.GuessMarket.class,
                assignmentOne);
        assertInstanceOf(GuessMarket.class, assignmentTwo);
        assertNotEquals(
                assignmentOne.getClass().getPackageName(),
                assignmentTwo.getClass().getPackageName());
    }

    private EngineException assertXmlParseError(Path xml) {
        EngineException exception = assertThrows(
                EngineException.class,
                () -> unmarshaller.unmarshal(xml));
        assertEquals(ErrorCode.XML_PARSE_ERROR, exception.getErrorCode());
        return exception;
    }

    private static List<String> userNames(List<GMUser> users) {
        return users.stream().map(GMUser::getName).toList();
    }

    private static List<Integer> initialCash(List<GMUser> users) {
        return users.stream().map(GMUser::getInitialCash).toList();
    }

    private static List<Integer> marketMakerEventIds(GMUser user) {
        return user.getGMMarketMaker().getEvent().stream()
                .map(guessmarket.engine.loading.jaxb.v2.Event::getId)
                .toList();
    }

    private static Path resourcePath(String resourcePath) throws URISyntaxException {
        URL resource = Assignment2XmlUnmarshallerTest.class
                .getClassLoader()
                .getResource(resourcePath);
        if (resource == null) {
            throw new AssertionError("Missing classpath resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }

    private static Path writeXml(Path directory, String fileName, String content)
            throws Exception {
        Path path = directory.resolve(fileName);
        Files.writeString(path, content);
        return path;
    }
}
