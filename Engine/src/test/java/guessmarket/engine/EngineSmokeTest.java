package guessmarket.engine;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.exception.EngineException;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free smoke test. Run with assertions enabled (-ea). */
public final class EngineSmokeTest {
    private EngineSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path valid = Files.createTempFile("guess-market-valid-", ".xml");
        Path invalid = Files.createTempFile("guess-market-invalid-", ".xml");
        Files.writeString(valid, validXml());
        Files.writeString(invalid, validXml().replace("<id>2</id>", "<id>1</id>"));

        GuessMarketEngine engine = EngineFactory.createEngine();
        assert !engine.isSystemLoaded();
        assert engine.loadSystem(valid).loadedEventCount() == 2;
        assert engine.getAllEvents().size() == 2;
        assert Math.abs(engine.getEventDetails(1).options().get(0).currentValue() - 0.5) < 1.0e-12;



        LoadResult loadResult = engine.loadSystem(valid);

        if (loadResult.loadedEventCount() == 0) {
            throw new IllegalStateException("XML load failed: " + loadResult);
        }


        PurchaseResult purchase = engine.purchaseShares(1, 1, 100);
        assert Math.abs(purchase.shareCost() - 62.01145) < 0.001;
        assert Math.abs(purchase.commission() - purchase.shareCost() * 0.05) < 1.0e-12;
        assert purchase.updatedEvent().tradesNewestFirst().size() == 1;

        boolean invalidRejected = false;
        try {
            engine.loadSystem(invalid);
        } catch (EngineException ex) {
            invalidRejected = true;
        }
        assert invalidRejected;
        assert engine.getAllEvents().size() == 2 : "Atomic loading must preserve the old system.";

        CloseEventResult close = engine.closeEvent(1, 1);
        assert close.closedEvent().status() == EventStatus.CLOSED;
        assert close.grossPayout() == 100.0;
        assert close.commission() == 0.0;

        engine.purchaseShares(2, 1, 20);
        CloseEventResult closeWithCommission = engine.closeEvent(2, 1);
        assert closeWithCommission.grossPayout() == 20.0;
        assert closeWithCommission.commission() == 2.0;
        assert closeWithCommission.netPayout() == 18.0;
        assert Math.abs(closeWithCommission.closedEvent().totalCommissionCollected() - 2.0) < 1.0e-12;

        System.out.println("Engine smoke test passed.");
    }

    private static String validXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Guess-Market>
                    <GM-events>
                        <GM-event name="Election Winner">
                            <id>1</id>
                            <description>Will candidate A win?</description>
                            <commission type="on-purchase">5</commission>
                            <GM-options>
                                <GM-option>Yes</GM-option>
                                <GM-option>No</GM-option>
                            </GM-options>
                            <GM-method><GM-LMSR><b>100</b></GM-LMSR></GM-method>
                        </GM-event>
                        <GM-event name="Rain Tomorrow">
                            <id>2</id>
                            <description>Will it rain tomorrow?</description>
                            <commission type="on-close">10</commission>
                            <GM-options>
                                <GM-option>Rain</GM-option>
                                <GM-option>No rain</GM-option>
                            </GM-options>
                            <GM-method><GM-LMSR><b>50</b></GM-LMSR></GM-method>
                        </GM-event>
                    </GM-events>
                </Guess-Market>
                """;
    }
}
