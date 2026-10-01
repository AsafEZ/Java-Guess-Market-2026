package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.EventView;
import guessmarket.protocol.OptionView;
import javafx.application.Platform;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDetailPaneTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(ready::countDown);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @Test
    void makerAndMethodDetermineAvailableActions() throws Exception {
        onFxThread(() -> {
            EventDetailPane pane = new EventDetailPane(new MarketApiClient(),
                    "Maker", (message, error) -> {}, result -> {});
            VBox body = (VBox) pane.getContent();
            VBox actions = (VBox) body.getChildren().get(4);
            FlowPane open = (FlowPane) actions.getChildren().get(0);
            FlowPane purchase = (FlowPane) actions.getChildren().get(1);
            FlowPane order = (FlowPane) actions.getChildren().get(2);
            FlowPane close = (FlowPane) actions.getChildren().get(3);

            pane.select(summary("NOT_STARTED", "LMSR", "Maker"));
            pane.update(details("NOT_STARTED", "LMSR", "Maker"));
            assertTrue(open.isVisible());
            assertFalse(purchase.isVisible());
            assertFalse(order.isVisible());
            assertFalse(close.isVisible());

            pane.update(details("ACTIVE", "LMSR", "Maker"));
            assertFalse(open.isVisible());
            assertTrue(purchase.isVisible());
            assertFalse(order.isVisible());
            assertTrue(close.isVisible());

            pane.update(details("ACTIVE", "ORDER_BOOK", "Maker"));
            assertFalse(purchase.isVisible());
            assertTrue(order.isVisible());

            pane.update(details("ACTIVE", "ORDER_BOOK", "Other"));
            assertFalse(open.isVisible());
            assertTrue(order.isVisible());
            assertFalse(close.isVisible());
        });
    }

    private static EventView summary(String status, String method, String maker) {
        return new EventView(1, "Event", "Description", status, method, 5,
                "ON_PURCHASE", 100, maker, List.of(new OptionView(1, "Yes")));
    }

    private static EventDetailsView details(String status, String method, String maker) {
        return new EventDetailsView(summary(status, method, maker), 0,
                null, null, "LMSR".equals(method) ? 10 : null,
                "ORDER_BOOK".equals(method) ? false : null,
                "ORDER_BOOK".equals(method) ? 100 : null,
                "ORDER_BOOK".equals(method) ? 1 : null,
                List.of(), List.of(), List.of(), List.of());
    }

    private static void onFxThread(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
