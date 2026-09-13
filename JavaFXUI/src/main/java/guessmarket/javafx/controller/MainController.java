package guessmarket.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

import java.util.Objects;

public final class MainController {
    @FXML
    private Button loadFileButton;

    @FXML
    private TextField loadedFilePathField;

    @FXML
    private ProgressBar loadProgressBar;

    @FXML
    private Label globalFeedbackLabel;

    @FXML
    private TabPane workspaceTabs;

    @FXML
    private EventsController eventsViewController;

    @FXML
    private UsersController usersViewController;

    @FXML
    private void initialize() {
        Objects.requireNonNull(loadFileButton, "loadFileButton");
        Objects.requireNonNull(loadedFilePathField, "loadedFilePathField");
        Objects.requireNonNull(loadProgressBar, "loadProgressBar");
        Objects.requireNonNull(globalFeedbackLabel, "globalFeedbackLabel");
        Objects.requireNonNull(workspaceTabs, "workspaceTabs");
        Objects.requireNonNull(eventsViewController, "eventsViewController");
        Objects.requireNonNull(usersViewController, "usersViewController");

        loadedFilePathField.setEditable(false);
        loadProgressBar.setManaged(false);
        loadProgressBar.setVisible(false);
        globalFeedbackLabel.setManaged(false);
        globalFeedbackLabel.setVisible(false);
    }
}
