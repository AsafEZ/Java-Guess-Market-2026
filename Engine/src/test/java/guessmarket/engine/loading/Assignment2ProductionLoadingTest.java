package guessmarket.engine.loading;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.impl.GuessMarketEngineImpl;
import guessmarket.engine.trading.orderbook.OrderBookTradingMechanism;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Assignment2ProductionLoadingTest {
    private static final String SMALL = "assignment2/xml/valid/small.xml";
    private static final String MULTIPLE = "assignment2/xml/valid/multiple.xml";

    @Test
    void smallXmlCreatesUsersNotStartedEventsAndMarketMakers() throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        LoadResult result = engine.loadSystem(resourcePath(SMALL));

        assertEquals(2, result.loadedEventCount());
        assertEquals(
                new LmsrCalculator().initialSubsidy(100),
                result.totalInitialSubsidy(),
                1.0e-12);
        assertEquals(
                List.of("Avrum", "Tikva", "Menash"),
                engine.getAllUsers().stream().map(user -> user.name()).toList());
        assertEquals(List.of(1000.0, 10000.0, 100.0),
                engine.getAllUsers().stream().map(user -> user.balance()).toList());
        assertTrue(engine.getAllMarketEvents().stream()
                .allMatch(event -> event.status() == EventStatus.NOT_STARTED));
        assertTrue(engine.getAllMarketEvents().stream()
                .allMatch(event -> event.accountBalance() == 0.0));
        assertEquals(
                List.of(TradingMethod.LMSR, TradingMethod.ORDER_BOOK),
                engine.getAllMarketEvents().stream()
                        .map(event -> event.tradingMethod()).toList());
        assertEquals("Tikva", engine.getAllMarketEvents().get(0)
                .marketMakerName().orElseThrow());
        assertEquals("Avrum", engine.getAllMarketEvents().get(1)
                .marketMakerName().orElseThrow());
    }

    @Test
    void multipleXmlSumsOnlyLmsrSubsidiesAndStoresOrderBookConfiguration()
            throws Exception {
        MarketSystemXmlLoader loader = new MarketSystemXmlLoader(
                new LmsrCalculator());

        MarketSystemXmlLoader.LoadedSystem loaded = loader.load(
                resourcePath(MULTIPLE));

        double expected = new LmsrCalculator().initialSubsidy(100)
                + new LmsrCalculator().initialSubsidy(200);
        assertEquals(expected, loaded.totalInitialSubsidy(), 1.0e-12);
        assertEquals(4, loaded.system().size());
        OrderBookTradingMechanism mechanism = assertInstanceOf(
                OrderBookTradingMechanism.class,
                loaded.system().getEvent(3).getTradingMechanism());
        assertEquals(false, mechanism.isMintAllowed());
        assertEquals(1000, mechanism.getInitialInvestment());
        assertEquals(1, mechanism.getD());
    }

    @Test
    void instructorBusinessInvalidFixturesUseFocusedErrors() throws Exception {
        assertLoadError(
                ErrorCode.INVALID_INITIAL_CASH,
                "assignment2/xml/invalid-business-rules/error-2.xml");
        assertLoadError(
                ErrorCode.EVENT_NOT_FOUND,
                "assignment2/xml/invalid-business-rules/error-3.xml");
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
    void schemaInvalidFixturesFailThroughProductionLoader(String fixture)
            throws Exception {
        assertLoadError(
                ErrorCode.XML_PARSE_ERROR,
                "assignment2/xml/invalid-schema/" + fixture);
    }

    @Test
    void failedReloadPreservesPreviouslyLoadedSystem() throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        engine.loadSystem(resourcePath(SMALL));

        EngineException exception = assertThrows(
                EngineException.class,
                () -> engine.loadSystem(resourcePath(
                        "assignment2/xml/invalid-business-rules/error-2.xml")));

        assertEquals(ErrorCode.INVALID_INITIAL_CASH, exception.getErrorCode());
        assertEquals(2, engine.getAllMarketEvents().size());
        assertEquals(3, engine.getAllUsers().size());
        assertEquals("Avrum", engine.getAllUsers().getFirst().name());
    }

    @Test
    void hybridMalformedAndDoctypeDocumentsAreRejected(@TempDir Path tempDir)
            throws Exception {
        Path hybrid = write(tempDir, "hybrid.xml", """
                <Guess-Market><GM-events><GM-event name="x"><id>1</id>
                <description>x</description><comision type="on-purchase">0</comision>
                <GM-options><GM-option>Y</GM-option><GM-option>N</GM-option></GM-options>
                <GM-method><GM-LMSR><b>1</b></GM-LMSR></GM-method></GM-event></GM-events>
                <GM-users><GM-user name="u"><initial-cash>1</initial-cash></GM-user></GM-users>
                </Guess-Market>
                """);
        Path malformed = write(tempDir, "malformed.xml", "<Guess-Market>");
        Path doctype = write(tempDir, "doctype.xml", """
                <!DOCTYPE Guess-Market [<!ENTITY xxe SYSTEM "file:///missing">]>
                <Guess-Market><GM-events>&xxe;</GM-events></Guess-Market>
                """);

        assertPathError(ErrorCode.XML_PARSE_ERROR, hybrid);
        assertPathError(ErrorCode.XML_PARSE_ERROR, malformed);
        assertPathError(ErrorCode.XML_PARSE_ERROR, doctype);
    }

    private static void assertLoadError(ErrorCode code, String resource)
            throws Exception {
        assertPathError(code, resourcePath(resource));
    }

    private static void assertPathError(ErrorCode code, Path path) {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        EngineException exception = assertThrows(
                EngineException.class,
                () -> engine.loadSystem(path));
        assertEquals(code, exception.getErrorCode());
    }

    private static Path write(Path directory, String name, String content)
            throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private static Path resourcePath(String resourcePath)
            throws URISyntaxException {
        URL resource = Assignment2ProductionLoadingTest.class
                .getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new AssertionError("Missing resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }
}
