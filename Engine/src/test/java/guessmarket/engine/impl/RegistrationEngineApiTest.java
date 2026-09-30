package guessmarket.engine.impl;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.Assignment3Engine;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationEngineApiTest {
    @Test
    void registrationBeforeUploadCreatesEmptyActiveAccount() {
        GuessMarketEngine engine = EngineFactory.createEngine();

        UserSummary user = engine.registerUser("  Visitor  ");

        assertEquals("Visitor", user.name());
        assertEquals(0.0, user.balance());
        assertEquals(UserStatus.ACTIVE, user.status());
        assertEquals(List.of(user), engine.getAllUsers());
        assertEquals(0.0, engine.getUserDetails("Visitor").balance());
        assertFalse(engine.isSystemLoaded());
    }

    @Test
    void duplicateNameIsRejectedWithoutAddingAnotherAccount() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Visitor");

        EngineException error = assertThrows(EngineException.class,
                () -> engine.registerUser(" Visitor "));

        assertEquals(ErrorCode.DUPLICATE_USER_NAME, error.getErrorCode());
        assertEquals(1, engine.getAllUsers().size());
    }

    @Test
    void invalidNameDoesNotCreateUser() {
        GuessMarketEngine engine = EngineFactory.createEngine();

        for (String name : new String[]{null, "", "   "}) {
            EngineException error = assertThrows(EngineException.class,
                    () -> engine.registerUser(name));
            assertEquals(ErrorCode.INVALID_USER_NAME, error.getErrorCode());
        }
        assertFalse(engine.isSystemLoaded());
    }

    @Test
    void creditAccountBeforeAndAfterUploadUpdatesOnlyItsOwner() throws Exception {
        Assignment3Engine engine = EngineFactory.createAssignment3Engine();
        engine.registerUser("Alice");
        engine.registerUser("Bob");

        assertEquals(25.5, engine.creditAccount("Alice", 25.5).balance());
        assertEquals(0.0, engine.getUserDetails("Bob").balance());
        try (var xml = getClass().getClassLoader().getResourceAsStream(
                "assignment3/xml/small.xml")) {
            assertTrue(xml != null);
            engine.uploadEvents(xml, "Alice");
            assertEquals(30.5, engine.creditAccount("Alice", 5.0).balance());
        }
    }

    @Test
    void invalidCreditDoesNotChangeBalance() {
        Assignment3Engine engine = EngineFactory.createAssignment3Engine();
        engine.registerUser("Alice");
        for (double amount : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            EngineException error = assertThrows(EngineException.class,
                    () -> engine.creditAccount("Alice", amount));
            assertEquals(ErrorCode.INVALID_CREDIT_AMOUNT, error.getErrorCode());
        }
        assertEquals(0.0, engine.getUserDetails("Alice").balance());
    }

    @Test
    void preexistingRegistrationsSurviveSuccessfulLegacyLoad() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Visitor");

        engine.loadSystem(smallXml());

        assertTrue(engine.isSystemLoaded());
        assertEquals(List.of("Avrum", "Tikva", "Menash", "Visitor"),
                engine.getAllUsers().stream().map(UserSummary::name).toList());
        assertEquals(0.0, engine.getUserDetails("Visitor").balance());
    }

    @Test
    void loadCollisionLeavesPriorRegistrationUntouched() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.registerUser("Avrum");

        EngineException error = assertThrows(EngineException.class,
                () -> engine.loadSystem(smallXml()));

        assertEquals(ErrorCode.DUPLICATE_USER_NAME, error.getErrorCode());
        assertFalse(engine.isSystemLoaded());
        assertEquals(List.of("Avrum"),
                engine.getAllUsers().stream().map(UserSummary::name).toList());
    }

    @Test
    void registrationAfterLoadUsesTheLiveMarketRegistry() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.loadSystem(smallXml());

        engine.registerUser("Visitor");

        assertEquals("Visitor", engine.getAllUsers().getLast().name());
        assertEquals(0.0, engine.getUserDetails("Visitor").balance());
        EngineException error = assertThrows(EngineException.class,
                () -> engine.registerUser("Avrum"));
        assertEquals(ErrorCode.DUPLICATE_USER_NAME, error.getErrorCode());
    }

    @Test
    void legacyReloadKeepsNameWithoutCarryingOldAccountState() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.loadSystem(smallXml());
        engine.registerUser("Visitor");

        engine.loadSystem(smallXml());

        assertEquals(0.0, engine.getUserDetails("Visitor").balance());
        assertTrue(engine.getUserDetails("Visitor").positions().isEmpty());
    }

    private static Path smallXml() throws URISyntaxException {
        return Path.of(RegistrationEngineApiTest.class.getClassLoader()
                .getResource("assignment2/xml/valid/small.xml").toURI());
    }
}
