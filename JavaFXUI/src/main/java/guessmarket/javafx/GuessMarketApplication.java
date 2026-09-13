package guessmarket.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public final class GuessMarketApplication extends Application {
    private static final String MAIN_VIEW = "/guessmarket/javafx/view/main-view.fxml";
    private static final String APPLICATION_CSS = "/guessmarket/javafx/css/application.css";

    @Override
    public void start(Stage stage) throws Exception {
        URL viewUrl = requireResource(MAIN_VIEW);
        URL stylesheetUrl = requireResource(APPLICATION_CSS);

        Parent root = FXMLLoader.load(viewUrl);
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());

        stage.setTitle("Guess Market");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    private static URL requireResource(String path) {
        URL resource = GuessMarketApplication.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Required JavaFX resource is missing: " + path);
        }
        return resource;
    }
}
