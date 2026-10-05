package guessmarket.client;

import guessmarket.client.net.ApiException;
import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.EventView;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.AccountActivityView;
import guessmarket.protocol.ChatMessageView;
import guessmarket.protocol.UserDetailsView;
import guessmarket.protocol.UserView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class MarketClientApplication extends Application {
    private static final double INITIAL_CONTENT_WIDTH_PX = 1270;
    private static final double INITIAL_CONTENT_HEIGHT_PX = 785;

    private final MarketApiClient api = new MarketApiClient();
    private final ObservableList<EventView> events = FXCollections.observableArrayList();
    private final ObservableList<UserView> users = FXCollections.observableArrayList();
    private final FilteredList<EventView> filteredEvents = new FilteredList<>(events);
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(
            runnable -> daemonThread(runnable, "guess-market-poll"));

    private Stage stage;
    private String currentUser;
    private Label feedback;
    private AccountDetailPane accountDetail;
    private ChatPane chatPane;
    private EventDetailPane eventDetail;
    private TableView<EventView> eventTable;
    private TableView<UserView> userTable;
    private ComboBox<String> methodFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> commissionFilter;
    private ProgressIndicator uploadProgress;
    private File lastDirectory;
    private boolean replacingEvents;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Guess Market");
        stage.setMinWidth(620);
        stage.setMinHeight(500);
        showLogin();
        stage.show();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        poller.shutdownNow();
        if (currentUser != null) {
            try {
                api.logout();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // Tomcat expires an abandoned session if the client cannot disconnect.
            }
        }
    }

    private void showLogin() {
        Label title = new Label("Guess Market");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Sign in");
        subtitle.getStyleClass().add("section-title");
        TextField name = new TextField();
        name.setPromptText("User name");
        name.setMaxWidth(320);
        Button signIn = new Button("Sign in");
        signIn.getStyleClass().add("primary-button");
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        error.setMaxWidth(320);
        signIn.setOnAction(ignored -> {
            String entered = name.getText().trim();
            if (entered.isEmpty()) {
                error.setText("Enter a user name.");
                return;
            }
            signIn.setDisable(true);
            error.setText("");
            runAsync(() -> api.login(entered), user -> {
                signedIn(user);
            }, failure -> {
                signIn.setDisable(false);
                error.setText(loginMessage(failure));
            });
        });
        name.setOnAction(ignored -> signIn.fire());
        VBox form = new VBox(14, title, subtitle, name, signIn, error);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(28));
        form.setMaxSize(420, 390);
        form.getStyleClass().add("login-form");
        BorderPane root = new BorderPane(form);
        root.getStyleClass().add("app-root");
        BorderPane.setAlignment(form, Pos.CENTER);
        Screen screen = Screen.getPrimary();
        Scene scene = new Scene(root,
                initialSceneSize(INITIAL_CONTENT_WIDTH_PX, screen.getOutputScaleX(),
                        screen.getVisualBounds().getWidth()),
                initialSceneSize(INITIAL_CONTENT_HEIGHT_PX, screen.getOutputScaleY(),
                        screen.getVisualBounds().getHeight()));
        addStyles(scene);
        stage.setScene(scene);
        Platform.runLater(name::requestFocus);
    }

    private void signedIn(UserView user) {
        currentUser = user.name();
        showWorkspace();
        startPolling();
    }

    private void showWorkspace() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createHeader());
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("workspace-tabs");
        tabs.getTabs().addAll(new Tab("Events", createEventsView()),
                new Tab("Account", createAccountView()));
        chatPane = new ChatPane(api, this::showFeedback);
        tabs.getTabs().add(new Tab("Chat", chatPane));
        root.setCenter(tabs);
        Scene current = stage.getScene();
        Scene scene = new Scene(root, current.getWidth(), current.getHeight());
        addStyles(scene);
        scene.widthProperty().addListener((ignored, oldValue, width) -> {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() instanceof SplitPane split) {
                    split.setOrientation(width.doubleValue() < 760
                            ? Orientation.VERTICAL : Orientation.HORIZONTAL);
                }
            }
        });
        stage.setScene(scene);
    }

    private VBox createHeader() {
        Label title = new Label("Guess Market");
        title.getStyleClass().add("app-title");
        Label userName = new Label(currentUser);
        userName.getStyleClass().add("current-user");
        userName.setMaxWidth(180);
        Button upload = new Button("Upload XML");
        upload.getStyleClass().add("primary-button");
        uploadProgress = new ProgressIndicator();
        uploadProgress.setPrefSize(20, 20);
        uploadProgress.setVisible(false);
        uploadProgress.setManaged(false);
        upload.setOnAction(ignored -> chooseAndUpload(upload));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, title, spacer, uploadProgress, upload, userName);
        row.setAlignment(Pos.CENTER_LEFT);
        feedback = new Label();
        feedback.setWrapText(true);
        feedback.setManaged(false);
        feedback.setVisible(false);
        VBox header = new VBox(8, row, feedback);
        header.setPadding(new Insets(12, 18, 10, 18));
        header.getStyleClass().add("application-header");
        return header;
    }

    private SplitPane createEventsView() {
        methodFilter = filter("All methods", "LMSR", "ORDER_BOOK");
        statusFilter = filter("All statuses", "NOT_STARTED", "ACTIVE", "CLOSED");
        commissionFilter = filter("All commissions", "ON_PURCHASE", "ON_CLOSE");
        methodFilter.setOnAction(ignored -> updateEventFilter());
        statusFilter.setOnAction(ignored -> updateEventFilter());
        commissionFilter.setOnAction(ignored -> updateEventFilter());
        FlowPane filters = new FlowPane(8, 8, methodFilter, statusFilter,
                commissionFilter);
        filters.setAlignment(Pos.CENTER_LEFT);
        eventTable = new TableView<>(filteredEvents);
        eventTable.setPlaceholder(new Label("No events yet"));
        eventTable.getColumns().setAll(List.of(
                column("Event", event -> event.name(), 200),
                column("Method", event -> event.tradingMethod(), 95),
                column("Status", event -> event.status(), 90),
                column("Market maker", event -> event.marketMakerName(), 120)));
        eventTable.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> {
                    if (!replacingEvents) {
                        eventDetail.select(selected);
                    }
                });
        Label section = new Label("Events");
        section.getStyleClass().add("section-title");
        VBox left = new VBox(10, section, filters, eventTable);
        left.setPadding(new Insets(16));
        left.getStyleClass().add("master-pane");
        VBox.setVgrow(eventTable, Priority.ALWAYS);
        eventDetail = new EventDetailPane(api, currentUser, this::showFeedback,
                result -> accountDetail.updateBalance(result.account().balance()));
        SplitPane split = new SplitPane(left, eventDetail);
        split.setDividerPositions(0.54);
        return split;
    }

    private SplitPane createAccountView() {
        userTable = new TableView<>(users);
        userTable.setPlaceholder(new Label("No users yet"));
        userTable.getColumns().setAll(List.of(
                column("User", user -> user.name(), 150),
                column("Balance", user -> money(user.balance()), 110),
                column("Market maker", user -> user.marketMaker() ? "Yes" : "No", 120)));
        Label usersTitle = new Label("Users");
        usersTitle.getStyleClass().add("section-title");
        VBox left = new VBox(10, usersTitle, userTable);
        left.setPadding(new Insets(16));
        left.getStyleClass().add("master-pane");
        VBox.setVgrow(userTable, Priority.ALWAYS);

        accountDetail = new AccountDetailPane(api, this::showFeedback);
        SplitPane split = new SplitPane(left, accountDetail);
        split.setDividerPositions(0.42);
        return split;
    }

    private void startPolling() {
        poller.scheduleWithFixedDelay(() -> {
            try {
                List<EventView> nextEvents = api.events();
                List<UserView> nextUsers = api.users();
                UserDetailsView account = api.account();
                List<AccountActivityView> activity = api.history();
                List<ChatMessageView> chat = fetchChat();
                int detailId = eventDetail.selectedId();
                EventDetailsView detail = detailId == 0 ? null : api.event(detailId);
                Platform.runLater(() -> {
                    int selectedId = eventDetail.selectedId();
                    if (!events.equals(nextEvents)) {
                        replacingEvents = true;
                        try {
                            events.setAll(nextEvents);
                            if (selectedId != 0) {
                                filteredEvents.stream()
                                        .filter(event -> event.eventId() == selectedId)
                                        .findFirst().ifPresent(event ->
                                                eventTable.getSelectionModel().select(event));
                            }
                        } finally {
                            replacingEvents = false;
                        }
                    }
                    TableRefresh.update(userTable, nextUsers, UserView::name);
                    accountDetail.update(account, activity);
                    chatPane.append(chat);
                    if (detail != null) {
                        eventDetail.update(detail);
                    }
                });
            } catch (Exception exception) {
                Platform.runLater(() -> showFeedback(message(exception), true));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private List<ChatMessageView> fetchChat() throws IOException, InterruptedException {
        try {
            return api.chatAfter(chatPane.lastId());
        } catch (ApiException exception) {
            if (exception.status() == 404) {
                return List.of();
            }
            throw exception;
        }
    }

    private void chooseAndUpload(Button upload) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Guess Market XML");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files", "*.xml"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        upload.setDisable(true);
        uploadProgress.setManaged(true);
        uploadProgress.setVisible(true);
        runAsync(() -> api.upload(file.toPath()), result -> {
            upload.setDisable(false);
            uploadProgress.setManaged(false);
            uploadProgress.setVisible(false);
            showFeedback("Uploaded " + result.uploadedEventCount() + " events.", false);
        }, failure -> {
            upload.setDisable(false);
            uploadProgress.setManaged(false);
            uploadProgress.setVisible(false);
            showFeedback(message(failure), true);
        });
    }

    private void updateEventFilter() {
        filteredEvents.setPredicate(event ->
                matches(methodFilter.getValue(), "All methods", event.tradingMethod())
                        && matches(statusFilter.getValue(), "All statuses", event.status())
                        && matches(commissionFilter.getValue(), "All commissions",
                        event.commissionType()));
    }

    private static boolean matches(String choice, String all, String actual) {
        return choice == null || choice.equals(all) || choice.equals(actual);
    }

    private static ComboBox<String> filter(String all, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add(all);
        combo.getItems().addAll(values);
        combo.setValue(all);
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        return combo;
    }

    private static <T> TableColumn<T, String> column(
            String title, java.util.function.Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Objects.toString(value.apply(cell.getValue()), "")));
        column.setPrefWidth(width);
        return column;
    }

    private void showFeedback(String text, boolean error) {
        feedback.setText(text);
        feedback.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedback.getStyleClass().add(error ? "feedback-error" : "feedback-success");
        feedback.setManaged(true);
        feedback.setVisible(true);
    }

    static String message(Throwable failure) {
        if (failure instanceof ApiException apiFailure) {
            return apiFailure.getMessage();
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConnectException
                    || cause instanceof HttpConnectTimeoutException) {
                return "Cannot connect to the Guess Market server at "
                        + MarketApiClient.DEFAULT_BASE + ". Start Tomcat and try again.";
            }
        }
        return failure.getMessage() == null ? "The request failed." : failure.getMessage();
    }

    static String loginMessage(Throwable failure) {
        if (failure instanceof ApiException apiFailure
                && "DUPLICATE_USER_NAME".equals(apiFailure.errorCode())) {
            return "That user name is already in use. Try another name.";
        }
        return message(failure);
    }

    static double initialSceneSize(double physicalPixels, double outputScale,
                                   double visualSize) {
        return Math.min(physicalPixels / outputScale, Math.max(1, visualSize - 40));
    }

    private static String money(double value) {
        return String.format(java.util.Locale.ROOT, "%,.2f", value);
    }

    private static void addStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(
                MarketClientApplication.class.getResource("/guessmarket/client/application.css"))
                .toExternalForm());
    }

    static <T> void runAsync(Callable<T> work, Consumer<T> success,
                                     Consumer<Throwable> failure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(ignored -> success.accept(task.getValue()));
        task.setOnFailed(ignored -> failure.accept(task.getException()));
        daemonThread(task, "guess-market-request").start();
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
