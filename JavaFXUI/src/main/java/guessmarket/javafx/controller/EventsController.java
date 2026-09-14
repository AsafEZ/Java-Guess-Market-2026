package guessmarket.javafx.controller;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.LimitOrderDetails;
import guessmarket.engine.dto.LmsrEventDetails;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.OptionDetails;
import guessmarket.engine.dto.OrderBookEventDetails;
import guessmarket.engine.dto.OrderBookOptionDetails;
import guessmarket.engine.dto.OrderExecutionDetails;
import guessmarket.engine.dto.PositionDetails;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.enums.TradingMethod;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class EventsController {
    private GuessMarketEngine engine;
    private List<MarketEventSummary> allEvents = List.of();

    @FXML private SplitPane eventsSplitPane;
    @FXML private ToggleGroup mechanismFilterGroup;
    @FXML private ToggleGroup statusFilterGroup;
    @FXML private ToggleGroup commissionFilterGroup;
    @FXML private TableView<MarketEventSummary> eventTable;
    @FXML private VBox eventEmptyState;
    @FXML private ScrollPane eventDetailsScrollPane;
    @FXML private TilePane optionAreasTilePane;
    @FXML private TableView<ParticipantRow> participantTable;
    @FXML private TableView<HistoryRow> eventTradeHistoryTable;
    @FXML private Label eventTitleLabel;
    @FXML private Label eventStatusLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private Label eventMarketMakerLabel;
    @FXML private Label eventAccountBalanceLabel;
    @FXML private Label eventCommissionLabel;
    @FXML private Label eventWinnerLabel;

    @FXML
    private void initialize() {
        Objects.requireNonNull(eventsSplitPane, "eventsSplitPane");
        Objects.requireNonNull(mechanismFilterGroup, "mechanismFilterGroup");
        Objects.requireNonNull(statusFilterGroup, "statusFilterGroup");
        Objects.requireNonNull(commissionFilterGroup, "commissionFilterGroup");
        Objects.requireNonNull(eventTable, "eventTable");
        Objects.requireNonNull(eventEmptyState, "eventEmptyState");
        Objects.requireNonNull(eventDetailsScrollPane, "eventDetailsScrollPane");
        Objects.requireNonNull(optionAreasTilePane, "optionAreasTilePane");
        Objects.requireNonNull(participantTable, "participantTable");
        Objects.requireNonNull(eventTradeHistoryTable, "eventTradeHistoryTable");
        configureEventTable();
        configureParticipantTable();
        configureHistoryTable();
        mechanismFilterGroup.selectedToggleProperty().addListener(
                (observable, previous, selected) -> applyFilters());
        statusFilterGroup.selectedToggleProperty().addListener(
                (observable, previous, selected) -> applyFilters());
        commissionFilterGroup.selectedToggleProperty().addListener(
                (observable, previous, selected) -> applyFilters());
        eventTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showEvent(selected));
        eventsSplitPane.widthProperty().addListener(
                (observable, previous, width) -> updateResponsiveLayout(width.doubleValue()));
        showDetails(false);
    }

    private void updateResponsiveLayout(double width) {
        if (width <= 0.0) {
            return;
        }
        boolean narrow = width < 800.0;
        Orientation target = narrow ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        if (eventsSplitPane.getOrientation() != target) {
            eventsSplitPane.setOrientation(target);
            eventsSplitPane.setDividerPositions(narrow ? 0.36 : 0.38);
        }
        optionAreasTilePane.setPrefTileWidth(
                narrow ? Math.max(250.0, width - 128.0) : 250.0);
    }

    public void initializeEngine(GuessMarketEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public void refresh() {
        if (engine == null || !engine.isSystemLoaded()) {
            return;
        }
        Integer selectedId = Optional.ofNullable(
                        eventTable.getSelectionModel().getSelectedItem())
                .map(MarketEventSummary::eventId)
                .orElse(null);
        allEvents = engine.getAllMarketEvents();
        applyFilters();
        if (selectedId != null) {
            eventTable.getItems().stream()
                    .filter(event -> event.eventId() == selectedId)
                    .findFirst()
                    .ifPresent(eventTable.getSelectionModel()::select);
        }
    }

    private void applyFilters() {
        String method = selectedText(mechanismFilterGroup);
        String status = selectedText(statusFilterGroup);
        String commission = selectedText(commissionFilterGroup);
        List<MarketEventSummary> filtered = allEvents.stream()
                .filter(event -> method.equals("All")
                        || display(event.tradingMethod()).equals(method))
                .filter(event -> status.equals("All")
                        || display(event.status()).equals(status))
                .filter(event -> commission.equals("All")
                        || display(event.commissionType()).equals(commission))
                .toList();
        eventTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showEvent(MarketEventSummary selected) {
        if (selected == null || engine == null) {
            showDetails(false);
            return;
        }
        MarketEventDetails details = engine.getMarketEventDetails(selected.eventId());
        eventTitleLabel.setText(details.name());
        eventStatusLabel.setText(display(details.status()));
        eventDescriptionLabel.setText(details.description());
        eventMarketMakerLabel.setText(details.marketMakerName().orElse("Not assigned"));
        eventAccountBalanceLabel.setText(money(details.accountBalance()));
        eventCommissionLabel.setText(
                details.commissionPercentage() + "% " + display(details.commissionType()));
        eventWinnerLabel.setText(details.winningOptionName().orElse("Not decided"));
        renderMechanism(details);
        renderParticipants(details);
        renderHistory(details);
        showDetails(true);
    }

    private void renderMechanism(MarketEventDetails details) {
        optionAreasTilePane.getChildren().clear();
        if (details.mechanismDetails() instanceof LmsrEventDetails lmsr) {
            optionAreasTilePane.setPrefTileHeight(170.0);
            for (OptionDetails option : lmsr.options()) {
                optionAreasTilePane.getChildren().add(lmsrOptionCard(option, lmsr.b()));
            }
        } else if (details.mechanismDetails() instanceof OrderBookEventDetails orderBook) {
            optionAreasTilePane.setPrefTileHeight(390.0);
            for (OrderBookOptionDetails option : orderBook.options()) {
                optionAreasTilePane.getChildren().add(orderBookCard(option, orderBook));
            }
        }
    }

    private Node lmsrOptionCard(OptionDetails option, int b) {
        VBox card = optionCard(option.name());
        card.getChildren().addAll(
                valueLabel("Option", Integer.toString(option.optionNumber())),
                valueLabel("Purchased shares", Long.toString(option.purchasedShares())),
                valueLabel("Current value", money(option.currentValue())),
                valueLabel("LMSR b", Integer.toString(b)));
        return card;
    }

    private Node orderBookCard(
            OrderBookOptionDetails option,
            OrderBookEventDetails orderBook) {
        VBox card = optionCard(option.optionName());
        FlowPane metrics = new FlowPane(
                10.0, 6.0,
                compactMetric("LAST", option.lastPrice()),
                compactMetric("BID", option.bestBid()),
                compactMetric("ASK", option.bestAsk()),
                compactMetric("MID", option.midPrice()),
                compactMetric("SPREAD", option.spread()));
        metrics.getStyleClass().add("market-metrics");
        Label configuration = new Label(
                "d=" + orderBook.d()
                        + "  Initial=" + orderBook.initialInvestment()
                        + "  Mint=" + (orderBook.mintAllowed() ? "Enabled" : "Disabled"));
        configuration.getStyleClass().add("muted-text");
        card.getChildren().addAll(
                configuration,
                metrics,
                new Label("Buy orders"),
                orderTable(option.buyOrders()),
                new Label("Sell orders"),
                orderTable(option.sellOrders()));
        return card;
    }

    private TableView<LimitOrderDetails> orderTable(List<LimitOrderDetails> orders) {
        TableView<LimitOrderDetails> table = new TableView<>(
                FXCollections.observableArrayList(orders));
        table.setPrefHeight(112.0);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<LimitOrderDetails, String> user = new TableColumn<>("User");
        user.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().userName()));
        TableColumn<LimitOrderDetails, String> quantity = new TableColumn<>("Qty");
        quantity.setCellValueFactory(cell -> new SimpleStringProperty(
                Long.toString(cell.getValue().remainingQuantity())));
        TableColumn<LimitOrderDetails, String> price = new TableColumn<>("Price");
        price.setCellValueFactory(cell -> new SimpleStringProperty(
                money(cell.getValue().limitPrice())));
        table.getColumns().addAll(List.of(user, quantity, price));
        table.setPlaceholder(new Label("No open orders"));
        return table;
    }

    private VBox optionCard(String title) {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("option-area");
        Label heading = new Label(title);
        heading.getStyleClass().add("option-title");
        card.getChildren().add(heading);
        return card;
    }

    private Label valueLabel(String name, String value) {
        Label label = new Label(name + ": " + value);
        label.getStyleClass().add("body-text");
        return label;
    }

    private VBox compactMetric(String name, Optional<Double> value) {
        Label title = new Label(name);
        title.getStyleClass().add("metric-name");
        Label number = new Label(value.map(EventsController::money).orElse("-"));
        number.getStyleClass().add("metric-value");
        return new VBox(2.0, title, number);
    }

    private void renderParticipants(MarketEventDetails details) {
        List<ParticipantRow> rows = details.participantPositions().stream()
                .map(position -> new ParticipantRow(
                        position.userName(),
                        position.marketMaker() ? "Market Maker" : "Participant",
                        shares(position, 0),
                        shares(position, 1)))
                .toList();
        participantTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void renderHistory(MarketEventDetails details) {
        List<HistoryRow> rows = new ArrayList<>();
        if (details.mechanismDetails() instanceof LmsrEventDetails lmsr) {
            for (TradeDetails trade : lmsr.tradesNewestFirst()) {
                rows.add(new HistoryRow(
                        Long.toString(trade.tradeNumber()), trade.optionName(),
                        Long.toString(trade.shareQuantity()), money(trade.shareCost()),
                        money(trade.commission()), money(trade.totalPaid())));
            }
        } else if (details.mechanismDetails() instanceof OrderBookEventDetails orderBook) {
            for (OrderExecutionDetails execution : orderBook.executionsNewestFirst()) {
                String parties = execution.buyerName()
                        + execution.sellerName().map(seller -> " / " + seller).orElse(" / MINT");
                rows.add(new HistoryRow(
                        Long.toString(execution.executionId()),
                        "Option " + execution.optionNumber() + "  " + parties,
                        Long.toString(execution.quantity()), money(execution.shareCost()),
                        "-", money(execution.shareCost())));
            }
        }
        eventTradeHistoryTable.setItems(FXCollections.observableArrayList(rows));
    }

    private static long shares(PositionDetails position, int index) {
        return position.options().size() > index
                ? position.options().get(index).shares() : 0L;
    }

    private void showDetails(boolean visible) {
        eventEmptyState.setManaged(!visible);
        eventEmptyState.setVisible(!visible);
        eventDetailsScrollPane.setManaged(visible);
        eventDetailsScrollPane.setVisible(visible);
    }

    private void configureEventTable() {
        stringColumn(eventTable, 0, MarketEventSummary::name);
        stringColumn(eventTable, 1, event -> display(event.status()));
        stringColumn(eventTable, 2, event -> display(event.tradingMethod()));
        stringColumn(eventTable, 3, event -> event.commissionPercentage()
                + "% " + display(event.commissionType()));
        stringColumn(eventTable, 4, event -> money(event.accountBalance()));
    }

    private void configureParticipantTable() {
        stringColumn(participantTable, 0, ParticipantRow::userName);
        stringColumn(participantTable, 1, ParticipantRow::role);
        stringColumn(participantTable, 2, row -> Long.toString(row.optionOneShares()));
        stringColumn(participantTable, 3, row -> Long.toString(row.optionTwoShares()));
    }

    private void configureHistoryTable() {
        stringColumn(eventTradeHistoryTable, 0, HistoryRow::id);
        stringColumn(eventTradeHistoryTable, 1, HistoryRow::option);
        stringColumn(eventTradeHistoryTable, 2, HistoryRow::quantity);
        stringColumn(eventTradeHistoryTable, 3, HistoryRow::shareCost);
        stringColumn(eventTradeHistoryTable, 4, HistoryRow::commission);
        stringColumn(eventTradeHistoryTable, 5, HistoryRow::total);
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

    private static String selectedText(ToggleGroup group) {
        return group.getSelectedToggle() instanceof ToggleButton button
                ? button.getText() : "All";
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

    private static String display(CommissionType type) {
        return type == CommissionType.ON_PURCHASE ? "Purchase" : "Close";
    }

    private static String money(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    private record ParticipantRow(
            String userName, String role, long optionOneShares, long optionTwoShares) {
    }

    private record HistoryRow(
            String id, String option, String quantity,
            String shareCost, String commission, String total) {
    }
}
