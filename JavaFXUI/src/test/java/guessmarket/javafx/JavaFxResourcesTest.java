package guessmarket.javafx;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.OrderBookEventDetails;
import guessmarket.engine.enums.EventStatus;
import guessmarket.javafx.controller.EventsController;
import guessmarket.javafx.controller.MainController;
import guessmarket.javafx.controller.UsersController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import javafx.geometry.Orientation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import javax.imageio.ImageIO;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        Platform.setImplicitExit(false);
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

    @Test
    void backgroundLoadWaitsWithoutBlockingAndPopulatesBothWorkspaces() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        Path fixture = assignment2Fixture("small.xml");
        LoadedView view = runOnJavaFxThread(() -> loadMainView(engine));

        long startedAt = System.nanoTime();
        runOnJavaFxThread(() -> {
            Method loadXml = MainController.class.getDeclaredMethod("loadXml", java.io.File.class);
            loadXml.setAccessible(true);
            loadXml.invoke(view.mainController(), fixture.toFile());
            assertTrue(field(view.mainController(), "loadFileButton", Button.class).isDisabled());
            return null;
        });

        waitUntil(() -> runOnJavaFxThread(() -> !field(
                view.mainController(), "loadedFilePathField", TextField.class)
                .getText().isBlank()));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        runOnJavaFxThread(() -> {
            assertTrue(elapsedMillis >= 900L, "The required loading delay was skipped.");
            assertEquals(fixture.toString(), field(
                    view.mainController(), "loadedFilePathField", TextField.class).getText());
            assertEquals(2, field(view.eventsController(), "eventTable", TableView.class)
                    .getItems().size());
            assertEquals(3, field(view.usersController(), "userTable", TableView.class)
                    .getItems().size());
            assertFalse(field(view.mainController(), "loadFileButton", Button.class).isDisabled());
            return null;
        });
    }

    @Test
    void failedBackgroundReloadPreservesTheLastSuccessfulWorkspace() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        Path valid = assignment2Fixture("valid", "small.xml");
        Path invalid = assignment2Fixture("invalid-business-rules", "error-2.xml");
        LoadedView view = runOnJavaFxThread(() -> loadMainView(engine));

        startBackgroundLoad(view, valid);
        waitUntil(() -> runOnJavaFxThread(() -> field(
                view.mainController(), "loadedFilePathField", TextField.class)
                .getText().equals(valid.toString())));
        startBackgroundLoad(view, invalid);
        waitUntil(() -> runOnJavaFxThread(() -> field(
                view.mainController(), "globalFeedbackLabel", Label.class)
                .getText().startsWith("Load failed:")));

        runOnJavaFxThread(() -> {
            assertEquals(valid.toString(), field(
                    view.mainController(), "loadedFilePathField", TextField.class).getText());
            assertEquals(2, field(view.eventsController(), "eventTable", TableView.class)
                    .getItems().size());
            assertEquals(3, field(view.usersController(), "userTable", TableView.class)
                    .getItems().size());
            assertEquals(2, engine.getAllMarketEvents().size());
            assertEquals(3, engine.getAllUsers().size());
            return null;
        });
    }

    @Test
    void userWorkspaceExecutesRequiredLmsrAndOrderBookWorkflows() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.loadSystem(assignment2Fixture("small.xml"));

        runOnJavaFxThread(() -> {
            LoadedView view = loadMainView(engine);
            UsersController users = view.usersController();
            TableView<?> usersTable = field(users, "userTable", TableView.class);
            TableView<?> marketsTable = field(users, "userEventsTable", TableView.class);

            usersTable.getSelectionModel().select(1); // Tikva, LMSR market maker
            marketsTable.getSelectionModel().select(0);
            field(users, "openEventButton", Button.class).fire();
            assertEquals(EventStatus.ACTIVE, engine.getMarketEventDetails(1).status());

            usersTable.getSelectionModel().select(2); // Menash, first-time participant
            marketsTable.getSelectionModel().select(0);
            field(users, "lmsrQuantityField", TextField.class).setText("2");
            field(users, "purchaseSharesButton", Button.class).fire();
            assertEquals(2L, engine.getUserDetails("Menash").positions().getFirst().totalShares());

            usersTable.getSelectionModel().select(1);
            marketsTable.getSelectionModel().select(0);
            field(users, "closeEventButton", Button.class).fire();
            assertEquals(EventStatus.CLOSED, engine.getMarketEventDetails(1).status());

            usersTable.getSelectionModel().select(0); // Avrum, Order Book market maker
            marketsTable.getSelectionModel().select(0); // Closed LMSR is filtered from Active
            field(users, "openEventButton", Button.class).fire();
            assertEquals(EventStatus.ACTIVE, engine.getMarketEventDetails(2).status());

            usersTable.getSelectionModel().select(2);
            marketsTable.getSelectionModel().select(0);
            field(users, "orderQuantityField", TextField.class).setText("3");
            field(users, "orderPriceField", TextField.class).setText("0.40");
            field(users, "submitOrderButton", Button.class).fire();
            OrderBookEventDetails orderBook = assertInstanceOf(
                    OrderBookEventDetails.class,
                    engine.getMarketEventDetails(2).mechanismDetails());
            assertEquals(1, orderBook.options().getFirst().buyOrders().size());

            usersTable.getSelectionModel().select(0);
            marketsTable.getSelectionModel().select(0);
            field(users, "closeEventButton", Button.class).fire();
            assertEquals(EventStatus.CLOSED, engine.getMarketEventDetails(2).status());
            return null;
        });
    }

    @Test
    void renderedWorkspacesRemainVisibleAtNormalAndNarrowWidths() throws Exception {
        GuessMarketEngine engine = EngineFactory.createEngine();
        engine.loadSystem(assignment2Fixture("small.xml"));

        runOnJavaFxThread(() -> {
            LoadedView view = loadMainView(engine);
            Parent root = view.root();
            Scene scene = new Scene(root, 1180, 780);
            scene.getStylesheets().add(resource(APPLICATION_CSS).toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Guess Market visual validation");
            try {
                stage.show();

                field(view.eventsController(), "eventTable", TableView.class)
                        .getSelectionModel().selectFirst();
                layout(root);
                writeSnapshot(root, "events-normal.png");

                TabPane tabs = field(view.mainController(), "workspaceTabs", TabPane.class);
                tabs.getSelectionModel().select(1);
                field(view.usersController(), "userTable", TableView.class)
                        .getSelectionModel().selectFirst();
                field(view.usersController(), "userEventsTable", TableView.class)
                        .getSelectionModel().selectFirst();
                layout(root);
                writeSnapshot(root, "users-normal.png");

                tabs.getSelectionModel().select(0);
                stage.setWidth(680.0);
                stage.setHeight(760.0);
                ((Region) root).resize(680.0, 760.0);
                layout(root);
                TilePane options = field(
                        view.eventsController(), "optionAreasTilePane", TilePane.class);
                assertEquals(Orientation.VERTICAL, field(
                        view.eventsController(), "eventsSplitPane",
                        javafx.scene.control.SplitPane.class).getOrientation());
                assertEquals(2, options.getChildren().size());
                assertTrue(options.getChildren().get(1).getLayoutY()
                                > options.getChildren().get(0).getLayoutY(),
                        "Option areas must stack vertically in a narrow window.");
                assertNoHorizontalScroll(field(
                        view.eventsController(), "eventDetailsScrollPane", ScrollPane.class));
                writeSnapshot(root, "events-narrow.png");

                tabs.getSelectionModel().select(1);
                layout(root);
                assertEquals(Orientation.VERTICAL, field(
                        view.usersController(), "usersSplitPane",
                        javafx.scene.control.SplitPane.class).getOrientation());
                assertNoHorizontalScroll(field(
                        view.usersController(), "userDetailsScrollPane", ScrollPane.class));
                writeSnapshot(root, "users-narrow.png");
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private static LoadedView loadMainView(GuessMarketEngine engine) throws Exception {
        FXMLLoader loader = new FXMLLoader(resource(MAIN_VIEW));
        Parent root = loader.load();
        MainController main = loader.getController();
        main.initializeEngine(engine);
        return new LoadedView(
                root,
                main,
                field(main, "eventsViewController", EventsController.class),
                field(main, "usersViewController", UsersController.class));
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

    private static Path assignment2Fixture(String fileName) {
        return assignment2Fixture("valid", fileName);
    }

    private static Path assignment2Fixture(String category, String fileName) {
        Path fromModule = Path.of(
                "..", "Engine", "src", "test", "resources",
                "assignment2", "xml", category, fileName).toAbsolutePath().normalize();
        Path fromRepository = Path.of(
                "Engine", "src", "test", "resources",
                "assignment2", "xml", category, fileName).toAbsolutePath().normalize();
        Path fixture = Files.exists(fromModule) ? fromModule : fromRepository;
        assertTrue(Files.exists(fixture), () -> "Missing Assignment 2 fixture: " + fixture);
        return fixture;
    }

    private static void startBackgroundLoad(LoadedView view, Path path) throws Exception {
        runOnJavaFxThread(() -> {
            Method loadXml = MainController.class.getDeclaredMethod("loadXml", java.io.File.class);
            loadXml.setAccessible(true);
            loadXml.invoke(view.mainController(), path.toFile());
            return null;
        });
    }

    private static <T> T field(Object owner, String name, Class<T> type)
            throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private static void waitUntil(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.call()) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("Timed out waiting for JavaFX operation.");
    }

    private static void layout(Parent root) {
        root.applyCss();
        root.layout();
    }

    private static void writeSnapshot(Parent root, String fileName) throws IOException {
        WritableImage image = root.snapshot(new SnapshotParameters(), null);
        BufferedImage output = new BufferedImage(
                (int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int firstPixel = image.getPixelReader().getArgb(0, 0);
        boolean hasDifferentPixel = false;
        for (int y = 0; y < output.getHeight(); y++) {
            for (int x = 0; x < output.getWidth(); x++) {
                int argb = image.getPixelReader().getArgb(x, y);
                output.setRGB(x, y, argb);
                hasDifferentPixel |= argb != firstPixel;
            }
        }
        assertTrue(hasDifferentPixel, "Rendered JavaFX snapshot must not be blank.");
        Path outputDirectory = Path.of("target", "visual-validation");
        Files.createDirectories(outputDirectory);
        Path outputPath = outputDirectory.resolve(fileName);
        assertTrue(ImageIO.write(output, "png", outputPath.toFile()));
        assertTrue(Files.size(outputPath) > 0L);
        assertNotEquals(0, output.getWidth());
        assertNotEquals(0, output.getHeight());
    }

    private static void assertNoHorizontalScroll(ScrollPane pane) {
        ScrollBar horizontal = (ScrollBar) pane.lookup(".scroll-bar:horizontal");
        assertTrue(horizontal == null || !horizontal.isVisible(),
                "Narrow details must not require horizontal scrolling.");
    }

    private static <T> T runOnJavaFxThread(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        return task.get(10, SECONDS);
    }

    private record LoadedView(
            Parent root,
            MainController mainController,
            EventsController eventsController,
            UsersController usersController) {
    }
}
