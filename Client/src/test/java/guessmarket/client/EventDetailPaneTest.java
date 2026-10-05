package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.EventView;
import guessmarket.protocol.OptionMarketView;
import guessmarket.protocol.OptionView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDetailPaneTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @Test
    void makerAndMethodDetermineAvailableActions() throws Exception {
        MarketApiClient api = new MarketApiClient();
        onFxThread(() -> {
            EventDetailPane pane = new EventDetailPane(api,
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

    @Test
    void periodicDetailsRefreshLeavesUnchangedRowsAloneAndKeepsSelection()
            throws Exception {
        onFxThread(() -> {
            EventDetailPane pane = new EventDetailPane(new MarketApiClient(),
                    "Maker", (message, error) -> {}, result -> {});
            VBox body = (VBox) pane.getContent();
            TabPane tabs = (TabPane) body.getChildren().get(5);
            @SuppressWarnings("unchecked")
            TableView<OptionMarketView> options =
                    (TableView<OptionMarketView>) tabs.getTabs().get(0).getContent();
            AtomicInteger changes = new AtomicInteger();
            options.getItems().addListener((ListChangeListener<OptionMarketView>)
                    ignored -> changes.incrementAndGet());

            pane.select(summary("ACTIVE", "LMSR", "Maker"));
            pane.update(detailsWithValue(0.5));
            options.getSelectionModel().selectFirst();
            OptionMarketView selected = options.getSelectionModel().getSelectedItem();
            int before = changes.get();

            pane.update(detailsWithValue(0.5));
            assertEquals(before, changes.get());
            assertSame(selected, options.getSelectionModel().getSelectedItem());

            pane.update(detailsWithValue(0.7));
            assertEquals(1, options.getSelectionModel().getSelectedItem().optionNumber());
            assertEquals(0.7,
                    options.getSelectionModel().getSelectedItem().currentValue().doubleValue());
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

    private static EventDetailsView detailsWithValue(double value) {
        OptionMarketView option = new OptionMarketView(1, "Yes", 0,
                value, value, null, null, null, null, List.of(), List.of());
        return new EventDetailsView(summary("ACTIVE", "LMSR", "Maker"), 0,
                null, null, 10, null, null, null,
                List.of(option), List.of(), List.of(), List.of());
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
