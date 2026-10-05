package guessmarket.client;

import javafx.scene.control.TableView;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class TableRefresh {
    private TableRefresh() {
    }

    static <T> boolean update(TableView<T> table, List<T> incoming,
                              Function<T, ?> identity) {
        if (table.getItems().equals(incoming)) {
            return false;
        }

        T selected = table.getSelectionModel().getSelectedItem();
        Object selectedId = selected == null ? null : identity.apply(selected);
        table.getItems().setAll(incoming);
        table.getSelectionModel().clearSelection();
        if (selected != null) {
            incoming.stream()
                    .filter(item -> Objects.equals(identity.apply(item), selectedId))
                    .findFirst()
                    .ifPresent(table.getSelectionModel()::select);
        }
        return true;
    }
}
