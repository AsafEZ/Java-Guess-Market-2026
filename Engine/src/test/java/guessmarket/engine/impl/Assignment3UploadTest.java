package guessmarket.engine.impl;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.EventUploadResult;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Assignment3UploadTest {
    @Test
    void smallSampleLoadsWithoutDiskAndAssignsUploaderAsMarketMaker() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");

        EventUploadResult result = engine.uploadEvents(resource("small.xml"), "Maker");

        assertEquals(1, result.uploadedEventCount());
        assertEquals(1, result.totalEventCount());
        assertEquals(List.of("Mujtaba is Dead"), result.eventNames());
        MarketEventSummary event = engine.getAllMarketEvents().getFirst();
        assertEquals(1, event.eventId());
        assertEquals(EventStatus.NOT_STARTED, event.status());
        assertEquals("Maker", event.marketMakerName().orElseThrow());
        assertTrue(engine.isSystemLoaded());
    }

    @Test
    void multipleSampleAccumulatesAndAssignsSecondUploader() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("First");
        engine.registerUser("Second");
        engine.uploadEvents(resource("small.xml"), "First");

        EventUploadResult result = engine.uploadEvents(resource("multiple.xml"), " Second ");

        assertEquals(3, result.uploadedEventCount());
        assertEquals(4, result.totalEventCount());
        assertEquals(List.of(1, 2, 3, 4), engine.getAllMarketEvents().stream()
                .map(MarketEventSummary::eventId).toList());
        assertEquals("First", engine.getAllMarketEvents().getFirst()
                .marketMakerName().orElseThrow());
        assertTrue(engine.getAllMarketEvents().stream().skip(1)
                .allMatch(event -> event.marketMakerName().orElseThrow().equals("Second")));
    }

    @Test
    void duplicateExistingNameRejectsWholeUpload() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");
        engine.uploadEvents(resource("small.xml"), "Maker");
        String duplicate = resourceText("multiple.xml")
                .replace("World Cap Winner", "Mujtaba is Dead");

        EngineException error = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml(duplicate), "Maker"));

        assertEquals(ErrorCode.DUPLICATE_EVENT_NAME, error.getErrorCode());
        assertEquals(1, engine.getAllMarketEvents().size());
    }

    @Test
    void duplicateNameWithinOneFileDoesNotPartiallyUpload() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");
        String duplicate = resourceText("multiple.xml")
                .replace("World Cap Winner", "Earth Quake on Dead Sea");

        EngineException error = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml(duplicate), "Maker"));

        assertEquals(ErrorCode.DUPLICATE_EVENT_NAME, error.getErrorCode());
        assertFalse(engine.isSystemLoaded());
    }

    @Test
    void invalidSchemaAndBusinessRulePreserveExistingEvents() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");
        engine.uploadEvents(resource("small.xml"), "Maker");
        String source = resourceText("multiple.xml");

        EngineException schemaError = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml(source.replace("<GM-options>", "<wrong-options>")),
                        "Maker"));
        EngineException businessError = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml(source.replace("<b>200</b>", "<b>0</b>")),
                        "Maker"));

        assertEquals(ErrorCode.XML_PARSE_ERROR, schemaError.getErrorCode());
        assertEquals(ErrorCode.INVALID_LMSR_B, businessError.getErrorCode());
        assertEquals(1, engine.getAllMarketEvents().size());
    }

    @Test
    void assignment2XmlAndUnknownUploaderAreRejected() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");

        EngineException unknownUser = assertThrows(EngineException.class,
                () -> engine.uploadEvents(resource("small.xml"), "Missing"));
        EngineException malformed = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml("<Guess-Market><GM-users/></Guess-Market>"),
                        "Maker"));

        assertEquals(ErrorCode.USER_NOT_FOUND, unknownUser.getErrorCode());
        assertEquals(ErrorCode.XML_PARSE_ERROR, malformed.getErrorCode());
        assertFalse(engine.isSystemLoaded());
    }

    @Test
    void documentTypeIsRejectedWithoutLoadingExternalResources() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Maker");
        String unsafe = "<!DOCTYPE Guess-Market [<!ENTITY xxe SYSTEM "
                + "\"file:///missing-secret\">]>" + resourceText("small.xml")
                .replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");

        EngineException error = assertThrows(EngineException.class,
                () -> engine.uploadEvents(xml(unsafe), "Maker"));

        assertEquals(ErrorCode.XML_PARSE_ERROR, error.getErrorCode());
        assertFalse(engine.isSystemLoaded());
    }

    private static InputStream resource(String name) {
        return Assignment3UploadTest.class.getClassLoader()
                .getResourceAsStream("assignment3/xml/" + name);
    }

    private static String resourceText(String name) {
        try (InputStream stream = resource(name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static InputStream xml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
