package guessmarket.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.Objects;

public final class EventsController {
    @FXML
    private ToggleGroup mechanismFilterGroup;

    @FXML
    private ToggleGroup statusFilterGroup;

    @FXML
    private ToggleGroup commissionFilterGroup;

    @FXML
    private TableView<?> eventTable;

    @FXML
    private VBox eventEmptyState;

    @FXML
    private ScrollPane eventDetailsScrollPane;

    @FXML
    private TilePane optionAreasTilePane;

    @FXML
    private TableView<?> participantTable;

    @FXML
    private TableView<?> eventTradeHistoryTable;

    @FXML
    private void initialize() {
        Objects.requireNonNull(mechanismFilterGroup, "mechanismFilterGroup");
        Objects.requireNonNull(statusFilterGroup, "statusFilterGroup");
        Objects.requireNonNull(commissionFilterGroup, "commissionFilterGroup");
        Objects.requireNonNull(eventTable, "eventTable");
        Objects.requireNonNull(eventEmptyState, "eventEmptyState");
        Objects.requireNonNull(eventDetailsScrollPane, "eventDetailsScrollPane");
        Objects.requireNonNull(optionAreasTilePane, "optionAreasTilePane");
        Objects.requireNonNull(participantTable, "participantTable");
        Objects.requireNonNull(eventTradeHistoryTable, "eventTradeHistoryTable");

        eventDetailsScrollPane.setManaged(false);
        eventDetailsScrollPane.setVisible(false);
    }
}
