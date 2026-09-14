package guessmarket.javafx.controller;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.exception.EngineException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Objects;

public final class MainController {
    private GuessMarketEngine engine;
    private File lastDirectory;
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
        loadFileButton.setOnAction(event -> chooseAndLoadXml());
    }

    public void initializeEngine(GuessMarketEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        eventsViewController.initializeEngine(engine);
        usersViewController.initializeEngine(engine, this::refreshAll, this::showFeedback);
        if (engine.isSystemLoaded()) {
            refreshAll();
        }
    }

    private void chooseAndLoadXml() {
        if (engine == null) {
            showFeedback("Engine is not available.", true);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Guess Market XML");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files", "*.xml"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        Window owner = loadFileButton.getScene() == null
                ? null : loadFileButton.getScene().getWindow();
        File selected = chooser.showOpenDialog(owner);
        if (selected == null) {
            return;
        }
        lastDirectory = selected.getParentFile();
        loadXml(selected);
    }

    void loadXml(File selected) {
        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() throws Exception {
                updateProgress(-1, 1);
                Thread.sleep(1_000L);
                return engine.loadSystem(selected.toPath());
            }
        };

        loadFileButton.setDisable(true);
        loadProgressBar.progressProperty().bind(task.progressProperty());
        setProgressVisible(true);
        showFeedback("Loading " + selected.getName() + "...", false);
        task.setOnSucceeded(event -> {
            finishLoading(task);
            LoadResult result = task.getValue();
            loadedFilePathField.setText(result.sourcePath().toString());
            refreshAll();
            showFeedback(
                    "Loaded " + result.loadedEventCount() + " events successfully.",
                    false);
        });
        task.setOnFailed(event -> {
            finishLoading(task);
            Throwable failure = task.getException();
            String message = failure instanceof EngineException engineFailure
                    ? engineFailure.getErrorCode() + ": " + engineFailure.getMessage()
                    : failure.getMessage();
            showFeedback("Load failed: " + message, true);
        });
        Thread worker = new Thread(task, "guess-market-xml-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishLoading(Task<?> task) {
        loadProgressBar.progressProperty().unbind();
        setProgressVisible(false);
        loadFileButton.setDisable(false);
    }

    private void setProgressVisible(boolean visible) {
        loadProgressBar.setManaged(visible);
        loadProgressBar.setVisible(visible);
    }

    private void refreshAll() {
        eventsViewController.refresh();
        usersViewController.refresh();
    }

    private void showFeedback(String message, boolean error) {
        globalFeedbackLabel.setText(message);
        globalFeedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        globalFeedbackLabel.getStyleClass().add(
                error ? "feedback-error" : "feedback-success");
        globalFeedbackLabel.setManaged(true);
        globalFeedbackLabel.setVisible(true);
    }
}
