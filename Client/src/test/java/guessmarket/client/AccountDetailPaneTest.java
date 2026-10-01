package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.AccountActivityView;
import guessmarket.protocol.OptionPositionView;
import guessmarket.protocol.PositionView;
import guessmarket.protocol.UserDetailsView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountDetailPaneTest {
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
    void accountRefreshShowsPositionsAndPrivateHistory() throws Exception {
        onFxThread(() -> {
            AccountDetailPane pane = new AccountDetailPane(new MarketApiClient(),
                    (message, error) -> {});
            VBox body = (VBox) pane.getContent();
            Label balance = (Label) body.getChildren().get(1);
            TabPane tabs = (TabPane) body.getChildren().get(3);
            VBox positionView = (VBox) tabs.getTabs().get(0).getContent();
            VBox historyView = (VBox) tabs.getTabs().get(1).getContent();
            @SuppressWarnings("unchecked")
            TableView<PositionView> positions = (TableView<PositionView>) positionView.getChildren().get(0);
            @SuppressWarnings("unchecked")
            TableView<OptionPositionView> options =
                    (TableView<OptionPositionView>) positionView.getChildren().get(2);
            @SuppressWarnings("unchecked")
            TableView<AccountActivityView> history =
                    (TableView<AccountActivityView>) historyView.getChildren().get(0);

            PositionView position = new PositionView("Alice", 7, "Election", "ACTIVE",
                    "LMSR", false, List.of(new OptionPositionView(1, "Yes", 3,
                    12, 1, false)), 3, 12, 1, List.of(), null, null);
            AccountActivityView activity = new AccountActivityView(1, "2026-10-01T10:00:00Z",
                    "DEPOSIT", null, null, 50, 0, 50);
            pane.update(new UserDetailsView("Alice", 50, "ACTIVE", List.of(),
                    List.of(position)), List.of(activity));
            positions.getSelectionModel().selectFirst();

            assertEquals("Balance: 50.00", balance.getText());
            assertEquals(1, positions.getItems().size());
            assertEquals("Yes", options.getItems().getFirst().name());
            assertEquals(1, history.getItems().size());
        });
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
