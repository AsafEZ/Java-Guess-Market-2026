package guessmarket.javafx.controller;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.OptionPositionDetails;
import guessmarket.engine.dto.OptionSummary;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.enums.UserStatus;
import guessmarket.engine.exception.EngineException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.geometry.Orientation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class UsersController {
    private GuessMarketEngine engine;
    private Runnable refreshAll = () -> { };
    private BiConsumer<String, Boolean> globalFeedback = (message, error) -> { };
    private UserDetails selectedUser;
    private MarketEventDetails selectedEvent;

    @FXML private SplitPane usersSplitPane;
    @FXML private TableView<UserSummary> userTable;
    @FXML private VBox userEmptyState;
    @FXML private ScrollPane userDetailsScrollPane;
    @FXML private ComboBox<String> participationScopeChoice;
    @FXML private TableView<UserEventRow> userEventsTable;
    @FXML private TableView<OptionPositionDetails> userOptionPositionsTable;
    @FXML private TableView<TradeDetails> userTradeHistoryTable;
    @FXML private VBox lmsrActionPane;
    @FXML private VBox orderBookActionPane;
    @FXML private Label selectedUserNameLabel;
    @FXML private Label selectedUserBalanceLabel;
    @FXML private Label selectedUserStatusLabel;
    @FXML private ComboBox<OptionSummary> lmsrOptionChoice;
    @FXML private TextField lmsrQuantityField;
    @FXML private Button purchaseSharesButton;
    @FXML private ToggleGroup orderSideGroup;
    @FXML private ToggleButton orderBuyToggle;
    @FXML private ToggleButton orderSellToggle;
    @FXML private ComboBox<OptionSummary> orderOptionChoice;
    @FXML private TextField orderQuantityField;
    @FXML private TextField orderPriceField;
    @FXML private Button submitOrderButton;
    @FXML private Button openEventButton;
    @FXML private ComboBox<OptionSummary> winningOptionChoice;
    @FXML private Button closeEventButton;
    @FXML private Label actionFeedbackLabel;

    @FXML
    private void initialize() {
        Objects.requireNonNull(usersSplitPane, "usersSplitPane");
        Objects.requireNonNull(userTable, "userTable");
        Objects.requireNonNull(userEmptyState, "userEmptyState");
        Objects.requireNonNull(userDetailsScrollPane, "userDetailsScrollPane");
        Objects.requireNonNull(participationScopeChoice, "participationScopeChoice");
        Objects.requireNonNull(userEventsTable, "userEventsTable");
        Objects.requireNonNull(userOptionPositionsTable, "userOptionPositionsTable");
        Objects.requireNonNull(userTradeHistoryTable, "userTradeHistoryTable");
        Objects.requireNonNull(lmsrActionPane, "lmsrActionPane");
        Objects.requireNonNull(orderBookActionPane, "orderBookActionPane");
        configureTables();
        configureOptionChoices();
        participationScopeChoice.getSelectionModel().selectFirst();
        participationScopeChoice.valueProperty().addListener(
                (observable, previous, selected) -> renderUserEvents());
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showUser(selected));
        userEventsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showUserEvent(selected));
        purchaseSharesButton.setOnAction(event -> purchaseLmsr());
        submitOrderButton.setOnAction(event -> submitOrder());
        openEventButton.setOnAction(event -> openEvent());
        closeEventButton.setOnAction(event -> closeEvent());
        orderBuyToggle.setSelected(true);
        usersSplitPane.widthProperty().addListener(
                (observable, previous, width) -> updateResponsiveLayout(width.doubleValue()));
        showUserDetails(false);
        showMechanismActions(null);
    }

    private void updateResponsiveLayout(double width) {
        if (width <= 0.0) {
            return;
        }
        boolean narrow = width < 800.0;
        Orientation target = narrow ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        if (usersSplitPane.getOrientation() != target) {
            usersSplitPane.setOrientation(target);
            usersSplitPane.setDividerPositions(narrow ? 0.34 : 0.28);
        }
    }

    public void initializeEngine(
            GuessMarketEngine engine,
            Runnable refreshAll,
            BiConsumer<String, Boolean> globalFeedback) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.refreshAll = Objects.requireNonNull(refreshAll, "refreshAll");
        this.globalFeedback = Objects.requireNonNull(globalFeedback, "globalFeedback");
    }

    public void refresh() {
        if (engine == null || !engine.isSystemLoaded()) {
            return;
        }
        String selectedName = Optional.ofNullable(
                        userTable.getSelectionModel().getSelectedItem())
                .map(UserSummary::name)
                .orElse(null);
        Integer selectedEventId = Optional.ofNullable(
                        userEventsTable.getSelectionModel().getSelectedItem())
                .map(row -> row.event().eventId())
                .orElse(null);
        userTable.setItems(FXCollections.observableArrayList(engine.getAllUsers()));
        if (selectedName != null) {
            userTable.getItems().stream()
                    .filter(user -> user.name().equals(selectedName))
                    .findFirst()
                    .ifPresent(userTable.getSelectionModel()::select);
        }
        if (selectedEventId != null) {
            userEventsTable.getItems().stream()
                    .filter(row -> row.event().eventId() == selectedEventId)
                    .findFirst()
                    .ifPresent(userEventsTable.getSelectionModel()::select);
        }
    }

    private void showUser(UserSummary summary) {
        if (summary == null || engine == null) {
            selectedUser = null;
            selectedEvent = null;
            showUserDetails(false);
            return;
        }
        selectedUser = engine.getUserDetails(summary.name());
        selectedEvent = null;
        selectedUserNameLabel.setText(selectedUser.name());
        selectedUserBalanceLabel.setText(money(selectedUser.balance()));
        selectedUserStatusLabel.setText(display(selectedUser.status()));
        renderUserEvents();
        userOptionPositionsTable.getItems().clear();
        userTradeHistoryTable.getItems().clear();
        showMechanismActions(null);
        showUserDetails(true);
    }

    private void renderUserEvents() {
        if (selectedUser == null || engine == null) {
            userEventsTable.getItems().clear();
            return;
        }
        Map<Integer, PositionDetails> positions = new LinkedHashMap<>();
        for (PositionDetails position : selectedUser.positions()) {
            positions.put(position.eventId(), position);
        }
        boolean activeOnly = "Active".equals(participationScopeChoice.getValue());
        List<UserEventRow> rows = engine.getAllMarketEvents().stream()
                .filter(event -> !activeOnly || event.status() != EventStatus.CLOSED)
                .map(event -> new UserEventRow(
                        event,
                        positions.get(event.eventId()),
                        roleFor(event, positions)))
                .toList();
        userEventsTable.setItems(FXCollections.observableArrayList(rows));
    }

    private String roleFor(
            MarketEventSummary event,
            Map<Integer, PositionDetails> positions) {
        boolean participant = positions.containsKey(event.eventId());
        boolean marketMaker = selectedUser.marketMakerEventIds().contains(event.eventId());
        if (participant && marketMaker) {
            return "Market Maker / Participant";
        }
        if (marketMaker) {
            return "Market Maker";
        }
        return participant ? "Participant" : "Available";
    }

    private void showUserEvent(UserEventRow row) {
        if (row == null || selectedUser == null) {
            selectedEvent = null;
            userOptionPositionsTable.getItems().clear();
            userTradeHistoryTable.getItems().clear();
            showMechanismActions(null);
            return;
        }
        selectedEvent = engine.getMarketEventDetails(row.event().eventId());
        PositionDetails position = selectedUser.positions().stream()
                .filter(candidate -> candidate.eventId() == selectedEvent.eventId())
                .findFirst()
                .orElse(null);
        userOptionPositionsTable.setItems(FXCollections.observableArrayList(
                position == null ? List.of() : position.options()));
        userTradeHistoryTable.setItems(FXCollections.observableArrayList(
                position == null ? List.of() : position.tradesNewestFirst()));
        populateOptions(selectedEvent.options());
        showMechanismActions(selectedEvent);
        updateActionAvailability();
    }

    private void populateOptions(List<OptionSummary> options) {
        lmsrOptionChoice.setItems(FXCollections.observableArrayList(options));
        orderOptionChoice.setItems(FXCollections.observableArrayList(options));
        winningOptionChoice.setItems(FXCollections.observableArrayList(options));
        lmsrOptionChoice.getSelectionModel().selectFirst();
        orderOptionChoice.getSelectionModel().selectFirst();
        winningOptionChoice.getSelectionModel().selectFirst();
    }

    private void showMechanismActions(MarketEventDetails event) {
        boolean lmsr = event != null && event.tradingMethod() == TradingMethod.LMSR;
        boolean orderBook = event != null && event.tradingMethod() == TradingMethod.ORDER_BOOK;
        setVisible(lmsrActionPane, lmsr);
        setVisible(orderBookActionPane, orderBook);
        updateActionAvailability();
    }

    private void updateActionAvailability() {
        boolean ready = selectedUser != null && selectedEvent != null;
        boolean activeUser = ready && selectedUser.status() == UserStatus.ACTIVE;
        boolean marketMaker = ready
                && selectedEvent.marketMakerName().filter(selectedUser.name()::equals).isPresent();
        boolean activeEvent = ready && selectedEvent.status() == EventStatus.ACTIVE;
        purchaseSharesButton.setDisable(!(activeUser && activeEvent
                && selectedEvent.tradingMethod() == TradingMethod.LMSR));
        submitOrderButton.setDisable(!(activeUser && activeEvent
                && selectedEvent.tradingMethod() == TradingMethod.ORDER_BOOK));
        openEventButton.setDisable(!(activeUser && marketMaker
                && selectedEvent.status() == EventStatus.NOT_STARTED));
        winningOptionChoice.setDisable(!(activeUser && marketMaker && activeEvent));
        closeEventButton.setDisable(!(activeUser && marketMaker && activeEvent));
    }

    private void purchaseLmsr() {
        runAction(() -> {
            OptionSummary option = requireOption(lmsrOptionChoice);
            long quantity = positiveLong(lmsrQuantityField.getText(), "Quantity");
            var result = engine.purchaseShares(
                    selectedEvent.eventId(), selectedUser.name(),
                    option.optionNumber(), quantity);
            return "Purchased " + result.shareQuantity() + " shares for "
                    + money(result.totalPaid()) + ".";
        });
    }

    private void submitOrder() {
        runAction(() -> {
            OptionSummary option = requireOption(orderOptionChoice);
            long quantity = positiveLong(orderQuantityField.getText(), "Quantity");
            double price = positiveDouble(orderPriceField.getText(), "Price");
            OrderSide side = orderSellToggle.isSelected() ? OrderSide.SELL : OrderSide.BUY;
            var result = engine.submitOrder(
                    selectedEvent.eventId(), selectedUser.name(),
                    option.optionNumber(), side, quantity, price);
            return "Order #" + result.submittedOrder().orderId()
                    + " accepted; " + result.executions().size() + " executions.";
        });
    }

    private void openEvent() {
        runAction(() -> {
            engine.openEvent(selectedEvent.eventId(), selectedUser.name());
            return "Event opened successfully.";
        });
    }

    private void closeEvent() {
        runAction(() -> {
            OptionSummary winner = requireOption(winningOptionChoice);
            var result = engine.closeEvent(
                    selectedEvent.eventId(), selectedUser.name(), winner.optionNumber());
            return "Event closed. Gross payout: " + money(result.totalGrossPayout()) + ".";
        });
    }

    private void runAction(Action action) {
        try {
            String message = action.run();
            showActionFeedback(message, false);
            globalFeedback.accept(message, false);
            refreshAll.run();
        } catch (EngineException exception) {
            String message = exception.getErrorCode() + ": " + exception.getMessage();
            showActionFeedback(message, true);
            globalFeedback.accept(message, true);
        } catch (IllegalArgumentException exception) {
            showActionFeedback(exception.getMessage(), true);
        }
    }

    private void showActionFeedback(String message, boolean error) {
        actionFeedbackLabel.setText(message);
        actionFeedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        actionFeedbackLabel.getStyleClass().add(
                error ? "feedback-error" : "feedback-success");
    }

    private void configureTables() {
        stringColumn(userTable, 0, UserSummary::name);
        stringColumn(userTable, 1, user -> money(user.balance()));
        stringColumn(userTable, 2, user -> display(user.status()));
        stringColumn(userEventsTable, 0, row -> row.event().name());
        stringColumn(userEventsTable, 1, row -> display(row.event().status()));
        stringColumn(userEventsTable, 2, UserEventRow::role);
        stringColumn(userEventsTable, 3, row -> display(row.event().tradingMethod()));
        stringColumn(userOptionPositionsTable, 0, OptionPositionDetails::optionName);
        stringColumn(userOptionPositionsTable, 1,
                option -> Long.toString(option.shares()));
        stringColumn(userOptionPositionsTable, 2,
                option -> money(option.amountPaid()));
        stringColumn(userOptionPositionsTable, 3,
                option -> money(option.commissionPaid()));
        stringColumn(userOptionPositionsTable, 4,
                option -> option.winningOption() ? "Yes" : "");
        stringColumn(userTradeHistoryTable, 0,
                trade -> Long.toString(trade.tradeNumber()));
        stringColumn(userTradeHistoryTable, 1, TradeDetails::optionName);
        stringColumn(userTradeHistoryTable, 2,
                trade -> Long.toString(trade.shareQuantity()));
        stringColumn(userTradeHistoryTable, 3,
                trade -> money(trade.shareCost()));
        stringColumn(userTradeHistoryTable, 4,
                trade -> money(trade.commission()));
        stringColumn(userTradeHistoryTable, 5,
                trade -> money(trade.totalPaid()));
    }

    private void configureOptionChoices() {
        StringConverter<OptionSummary> converter = new StringConverter<>() {
            @Override
            public String toString(OptionSummary option) {
                return option == null ? "" : option.optionNumber() + " - " + option.name();
            }

            @Override
            public OptionSummary fromString(String value) {
                return null;
            }
        };
        lmsrOptionChoice.setConverter(converter);
        orderOptionChoice.setConverter(converter);
        winningOptionChoice.setConverter(converter);
    }

    private void showUserDetails(boolean visible) {
        userEmptyState.setManaged(!visible);
        userEmptyState.setVisible(!visible);
        userDetailsScrollPane.setManaged(visible);
        userDetailsScrollPane.setVisible(visible);
    }

    private static void setVisible(VBox pane, boolean visible) {
        pane.setManaged(visible);
        pane.setVisible(visible);
    }

    private static OptionSummary requireOption(ComboBox<OptionSummary> choice) {
        OptionSummary option = choice.getValue();
        if (option == null) {
            throw new IllegalArgumentException("Select an option.");
        }
        return option;
    }

    private static long positiveLong(String text, String name) {
        try {
            long value = Long.parseLong(text.trim());
            if (value <= 0L) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(name + " must be a positive whole number.");
        }
    }

    private static double positiveDouble(String text, String name) {
        try {
            double value = Double.parseDouble(text.trim());
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(name + " must be a positive finite number.");
        }
    }

    private static <T> void stringColumn(
            TableView<T> table,
            int index,
            java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = column(table, index);
        column.setCellValueFactory(cell -> new SimpleStringProperty(
                value.apply(cell.getValue())));
    }

    @SuppressWarnings("unchecked")
    private static <T> TableColumn<T, String> column(TableView<T> table, int index) {
        return (TableColumn<T, String>) table.getColumns().get(index);
    }

    private static String display(UserStatus status) {
        return status == UserStatus.ACTIVE ? "Active" : "Blocked";
    }

    private static String display(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> "Not Started";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }

    private static String display(TradingMethod method) {
        return method == TradingMethod.LMSR ? "LMSR" : "Order Book";
    }

    private static String money(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    @FunctionalInterface
    private interface Action {
        String run();
    }

    private record UserEventRow(
            MarketEventSummary event,
            PositionDetails position,
            String role) {
    }
}
