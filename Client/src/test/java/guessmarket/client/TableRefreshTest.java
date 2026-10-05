package guessmarket.client;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRefreshTest {
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
    void unchangedRowsDoNotReplaceItemsOrSelection() throws Exception {
        onFxThread(() -> {
            TableView<Row> table = new TableView<>();
            AtomicInteger changes = new AtomicInteger();
            table.getItems().addListener((ListChangeListener<Row>) ignored -> changes.incrementAndGet());
            Row first = new Row(1, "First");
            assertTrue(TableRefresh.update(table, List.of(first), Row::id));
            table.getSelectionModel().selectFirst();
            int before = changes.get();

            assertFalse(TableRefresh.update(table, List.of(new Row(1, "First")), Row::id));
            assertEquals(before, changes.get());
            assertSame(first, table.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    void changedRowsRestoreSelectionByStableIdAndClearRemovedSelection()
            throws Exception {
        onFxThread(() -> {
            TableView<Row> table = new TableView<>();
            TableRefresh.update(table, List.of(new Row(1, "First"),
                    new Row(2, "Second")), Row::id);
            table.getSelectionModel().select(1);

            Row changed = new Row(2, "Updated");
            assertTrue(TableRefresh.update(table,
                    List.of(changed, new Row(1, "First")), Row::id));
            assertSame(changed, table.getSelectionModel().getSelectedItem());

            assertTrue(TableRefresh.update(table, List.of(new Row(1, "First")), Row::id));
            assertTrue(table.getSelectionModel().isEmpty());
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

    private record Row(int id, String name) {
    }
}
