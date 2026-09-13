package guessmarket.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class MainController {
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        if (statusLabel == null) {
            throw new IllegalStateException("The status label was not injected from FXML.");
        }
    }
}
