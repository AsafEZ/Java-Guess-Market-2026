package guessmarket.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.Objects;

public final class UsersController {
    @FXML
    private TableView<?> userTable;

    @FXML
    private VBox userEmptyState;

    @FXML
    private ScrollPane userDetailsScrollPane;

    @FXML
    private ComboBox<String> participationScopeChoice;

    @FXML
    private TableView<?> userEventsTable;

    @FXML
    private TableView<?> userOptionPositionsTable;

    @FXML
    private TableView<?> userTradeHistoryTable;

    @FXML
    private VBox lmsrActionPane;

    @FXML
    private VBox orderBookActionPane;

    @FXML
    private void initialize() {
        Objects.requireNonNull(userTable, "userTable");
        Objects.requireNonNull(userEmptyState, "userEmptyState");
        Objects.requireNonNull(userDetailsScrollPane, "userDetailsScrollPane");
        Objects.requireNonNull(participationScopeChoice, "participationScopeChoice");
        Objects.requireNonNull(userEventsTable, "userEventsTable");
        Objects.requireNonNull(userOptionPositionsTable, "userOptionPositionsTable");
        Objects.requireNonNull(userTradeHistoryTable, "userTradeHistoryTable");
        Objects.requireNonNull(lmsrActionPane, "lmsrActionPane");
        Objects.requireNonNull(orderBookActionPane, "orderBookActionPane");

        participationScopeChoice.getSelectionModel().selectFirst();
        userDetailsScrollPane.setManaged(false);
        userDetailsScrollPane.setVisible(false);
        orderBookActionPane.setManaged(false);
        orderBookActionPane.setVisible(false);
    }
}
