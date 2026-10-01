package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.ChatMessageView;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPaneTest {
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
    void overlappingPollsDoNotDuplicateChatMessages() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                ChatPane pane = new ChatPane(new MarketApiClient(), (message, error) -> {});
                ChatMessageView first = new ChatMessageView(1, "Alice", "Hello", 1000);
                ChatMessageView second = new ChatMessageView(2, "Bob", "Hi", 2000);
                pane.append(List.of(first, second));
                pane.append(List.of(second));
                @SuppressWarnings("unchecked")
                ListView<ChatMessageView> list =
                        (ListView<ChatMessageView>) pane.getChildren().get(1);
                assertEquals(2, list.getItems().size());
                assertEquals(2, pane.lastId());
            } catch (Throwable exception) {
                failure.set(exception);
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
