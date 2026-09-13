package guessmarket.javafx;

import guessmarket.javafx.controller.EventsController;
import guessmarket.javafx.controller.MainController;
import guessmarket.javafx.controller.UsersController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxResourcesTest {
    private static final String MAIN_VIEW = "/guessmarket/javafx/view/main-view.fxml";
    private static final String EVENTS_VIEW = "/guessmarket/javafx/view/events-view.fxml";
    private static final String USERS_VIEW = "/guessmarket/javafx/view/users-view.fxml";
    private static final String APPLICATION_CSS = "/guessmarket/javafx/css/application.css";

    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, SECONDS), "JavaFX toolkit did not start in time.");
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        Platform.exit();
    }

    @Test
    void requiredJavaFxResourcesAreAvailableOnTheClasspath() {
        assertAll(
                () -> assertNotNull(resource(MAIN_VIEW)),
                () -> assertNotNull(resource(EVENTS_VIEW)),
                () -> assertNotNull(resource(USERS_VIEW)),
                () -> assertNotNull(resource(APPLICATION_CSS)));
    }

    @Test
    void mainViewLoadsIncludedViewsAndInjectsAllControllers() throws Exception {
        runOnJavaFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(resource(MAIN_VIEW));
            Parent root = loader.load();
            MainController controller = loader.getController();

            assertNotNull(root);
            assertFxmlFieldsInjected(controller);
            return null;
        });
    }

    @Test
    void featureViewsLoadIndependentlyAndInjectTheirControllers() throws Exception {
        runOnJavaFxThread(() -> {
            assertViewLoadsAndInjects(EVENTS_VIEW, EventsController.class);
            assertViewLoadsAndInjects(USERS_VIEW, UsersController.class);
            return null;
        });
    }

    private static <T> void assertViewLoadsAndInjects(
            String resourcePath,
            Class<T> controllerType) throws Exception {
        FXMLLoader loader = new FXMLLoader(resource(resourcePath));
        Parent root = loader.load();
        T controller = controllerType.cast(loader.getController());

        assertNotNull(root);
        assertFxmlFieldsInjected(controller);
    }

    private static void assertFxmlFieldsInjected(Object controller) throws IllegalAccessException {
        assertNotNull(controller);
        for (Field field : controller.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(FXML.class)) {
                field.setAccessible(true);
                assertNotNull(
                        field.get(controller),
                        () -> "FXML field was not injected: "
                                + controller.getClass().getSimpleName()
                                + "."
                                + field.getName());
            }
        }
    }

    private static URL resource(String path) {
        return JavaFxResourcesTest.class.getResource(path);
    }

    private static <T> T runOnJavaFxThread(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        return task.get(10, SECONDS);
    }
}
