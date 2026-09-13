package guessmarket.javafx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaFxResourcesTest {
    @Test
    void requiredJavaFxResourcesAreAvailableOnTheClasspath() {
        assertAll(
                () -> assertNotNull(
                        JavaFxResourcesTest.class.getResource(
                                "/guessmarket/javafx/view/main-view.fxml")),
                () -> assertNotNull(
                        JavaFxResourcesTest.class.getResource(
                                "/guessmarket/javafx/css/application.css")));
    }
}
