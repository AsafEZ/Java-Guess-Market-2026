package guessmarket.engine.api;

import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.OrderSubmissionResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.OrderSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineApiCompatibilityTest {

    @Test
    void assignmentOneSignaturesRemainExact() throws NoSuchMethodException {
        assertMethod("loadSystem", LoadResult.class, Path.class);
        assertListMethod("getAllEvents", EventSummary.class);
        assertListMethod("getActiveEvents", EventSummary.class);
        assertMethod("getEventDetails", EventDetails.class, int.class);
        assertMethod(
                "purchaseShares",
                PurchaseResult.class,
                int.class,
                int.class,
                long.class);
        assertMethod("closeEvent", CloseEventResult.class, int.class, int.class);
        assertMethod("isSystemLoaded", boolean.class);
    }

    @Test
    void assignmentTwoSignaturesUseOnlyPublicDtoTypes() throws NoSuchMethodException {
        assertListMethod("getAllMarketEvents", MarketEventSummary.class);
        assertMethod("getMarketEventDetails", MarketEventDetails.class, int.class);
        assertListMethod("getAllUsers", UserSummary.class);
        assertMethod("getUserDetails", UserDetails.class, String.class);
        assertMethod("openEvent", MarketEventDetails.class, int.class, String.class);
        assertMethod(
                "purchaseShares",
                UserPurchaseResult.class,
                int.class,
                String.class,
                int.class,
                long.class);
        assertMethod(
                "closeEvent",
                SettlementResult.class,
                int.class,
                String.class,
                int.class);
        assertMethod(
                "submitOrder",
                OrderSubmissionResult.class,
                int.class,
                String.class,
                int.class,
                OrderSide.class,
                long.class,
                double.class);

        assertEquals(15, GuessMarketEngine.class.getDeclaredMethods().length);
        for (Method method : GuessMarketEngine.class.getDeclaredMethods()) {
            assertFalse(method.toGenericString().contains("guessmarket.engine.domain"));
        }
    }

    @Test
    void purchaseAndCloseOverloadsResolveToTheirExpectedResultTypes()
            throws NoSuchMethodException {
        Method legacyPurchase = GuessMarketEngine.class.getMethod(
                "purchaseShares", int.class, int.class, long.class);
        Method userPurchase = GuessMarketEngine.class.getMethod(
                "purchaseShares", int.class, String.class, int.class, long.class);
        Method legacyClose = GuessMarketEngine.class.getMethod(
                "closeEvent", int.class, int.class);
        Method userClose = GuessMarketEngine.class.getMethod(
                "closeEvent", int.class, String.class, int.class);

        assertEquals(PurchaseResult.class, legacyPurchase.getReturnType());
        assertEquals(UserPurchaseResult.class, userPurchase.getReturnType());
        assertEquals(CloseEventResult.class, legacyClose.getReturnType());
        assertEquals(SettlementResult.class, userClose.getReturnType());
    }

    @Test
    void assignmentOneXmlAndLegacyDtoFlowRemainOperational(@TempDir Path tempDir)
            throws Exception {
        Path xml = tempDir.resolve("legacy-system.xml");
        Files.writeString(xml, legacyXml());
        GuessMarketEngine engine = EngineFactory.createEngine();

        LoadResult load = engine.loadSystem(xml);
        List<EventSummary> allEvents = engine.getAllEvents();
        List<EventSummary> activeEvents = engine.getActiveEvents();
        EventDetails details = engine.getEventDetails(1);
        PurchaseResult purchase = engine.purchaseShares(1, 1, 2L);
        CloseEventResult close = engine.closeEvent(1, 1);

        assertEquals(1, load.loadedEventCount());
        assertEquals(1, allEvents.size());
        assertEquals(1, activeEvents.size());
        assertInstanceOf(EventSummary.class, allEvents.getFirst());
        assertInstanceOf(EventDetails.class, details);
        assertEquals(2L, purchase.shareQuantity());
        assertEquals(EventStatus.CLOSED, close.closedEvent().status());
        assertTrue(engine.getActiveEvents().isEmpty());
    }

    private static void assertMethod(
            String name,
            Class<?> returnType,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = GuessMarketEngine.class.getMethod(name, parameterTypes);
        assertEquals(returnType, method.getReturnType());
    }

    private static void assertListMethod(String name, Class<?> elementType)
            throws NoSuchMethodException {
        Method method = GuessMarketEngine.class.getMethod(name);
        assertEquals(List.class, method.getReturnType());
        assertEquals(
                "java.util.List<" + elementType.getName() + ">",
                method.getGenericReturnType().getTypeName());
    }

    private static String legacyXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Guess-Market>
                    <GM-events>
                        <GM-event name="Legacy event">
                            <id>1</id>
                            <description>Compatibility test</description>
                            <comision type="on-close">10</comision>
                            <GM-options>
                                <GM-option>Yes</GM-option>
                                <GM-option>No</GM-option>
                            </GM-options>
                            <GM-method><GM-LMSR><b>10</b></GM-LMSR></GM-method>
                        </GM-event>
                    </GM-events>
                </Guess-Market>
                """;
    }
}
