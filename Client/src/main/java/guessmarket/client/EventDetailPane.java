package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.ActionResultView;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.EventView;
import guessmarket.protocol.ExecutionView;
import guessmarket.protocol.OptionMarketView;
import guessmarket.protocol.OptionView;
import guessmarket.protocol.OrderView;
import guessmarket.protocol.PositionView;
import guessmarket.protocol.TradeView;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class EventDetailPane extends ScrollPane {
    private final MarketApiClient api;
    private final String userName;
    private final BiConsumer<String, Boolean> feedback;
    private final Consumer<ActionResultView> onAction;
    private final Label title = new Label("Select an event");
    private final Label description = new Label();
    private final Label metrics = new Label();
    private final Label state = new Label();
    private final ComboBox<OptionView> tradeOption = optionChooser();
    private final ComboBox<OptionView> orderOption = optionChooser();
    private final ComboBox<OptionView> winningOption = optionChooser();
    private final TextField quantity = new TextField("1");
    private final TextField orderQuantity = new TextField("1");
    private final TextField price = new TextField();
    private final ComboBox<String> side = new ComboBox<>(
            FXCollections.observableArrayList("BUY", "SELL"));
    private final Button openButton = command("Open event");
    private final Button purchaseButton = command("Buy shares");
    private final Button orderButton = command("Submit order");
    private final Button closeButton = command("Close event");
    private final FlowPane openRow = new FlowPane(8, 8, openButton);
    private final FlowPane purchaseRow = new FlowPane(8, 8,
            new Label("Option"), tradeOption, new Label("Quantity"), quantity,
            purchaseButton);
    private final FlowPane orderRow = new FlowPane(8, 8,
            new Label("Option"), orderOption, new Label("Side"), side,
            new Label("Quantity"), orderQuantity,
            new Label("Limit price"), price, orderButton);
    private final FlowPane closeRow = new FlowPane(8, 8,
            new Label("Winner"), winningOption, closeButton);
    private final VBox actions = new VBox(10, openRow, purchaseRow, orderRow, closeRow);
    private final TableView<OptionMarketView> optionTable = new TableView<>();
    private final TableView<OrderView> orderTable = new TableView<>();
    private final TableView<ExecutionView> executionTable = new TableView<>();
    private final TableView<TradeView> tradeTable = new TableView<>();
    private final TableView<PositionView> positionTable = new TableView<>();
    private final TabPane dataTabs = new TabPane();

    private EventDetailsView current;
    private volatile int selectedId;
    private boolean busy;

    EventDetailPane(MarketApiClient api, String userName,
                    BiConsumer<String, Boolean> feedback,
                    Consumer<ActionResultView> onAction) {
        this.api = Objects.requireNonNull(api);
        this.userName = Objects.requireNonNull(userName);
        this.feedback = Objects.requireNonNull(feedback);
        this.onAction = Objects.requireNonNull(onAction);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("detail-scroll");

        title.getStyleClass().add("section-title");
        title.setWrapText(true);
        description.setWrapText(true);
        metrics.setWrapText(true);
        state.getStyleClass().add("event-state");
        side.setValue("BUY");
        quantity.setPrefColumnCount(5);
        orderQuantity.setPrefColumnCount(5);
        price.setPrefColumnCount(6);
        openButton.setOnAction(ignored -> {
            int eventId = selectedId;
            submit(() -> api.open(eventId));
        });
        purchaseButton.setOnAction(ignored -> purchase());
        orderButton.setOnAction(ignored -> order());
        closeButton.setOnAction(ignored -> close());
        configureTables();

        VBox body = new VBox(12, title, state, description, metrics,
                actions, dataTabs);
        body.setPadding(new Insets(18));
        body.getStyleClass().add("detail-pane");
        setContent(body);
        showControls(false);
    }

    void select(EventView event) {
        int nextId = event == null ? 0 : event.eventId();
        if (nextId == selectedId) {
            return;
        }
        selectedId = nextId;
        current = null;
        title.setText(event == null ? "Select an event" : event.name());
        state.setText(event == null ? "" : event.status());
        description.setText("");
        metrics.setText("");
        clearTables();
        showControls(false);
    }

    int selectedId() {
        return selectedId;
    }

    void update(EventDetailsView details) {
        if (details == null || details.summary().eventId() != selectedId
                || details.equals(current)) {
            return;
        }
        boolean newEvent = current == null;
        current = details;
        EventView summary = details.summary();
        title.setText(summary.name());
        state.setText(summary.status().replace('_', ' ') + "  |  "
                + summary.tradingMethod().replace('_', ' '));
        description.setText(summary.description());
        metrics.setText("Market maker: " + summary.marketMakerName()
                + "     Event balance: " + money(summary.accountBalance())
                + "     Commission: " + summary.commissionPercentage() + "% "
                + summary.commissionType().replace('_', ' ')
                + (details.winningOptionName() == null ? ""
                : "     Winner: " + details.winningOptionName()));
        if (newEvent) {
            tradeOption.getItems().setAll(summary.options());
            orderOption.getItems().setAll(summary.options());
            winningOption.getItems().setAll(summary.options());
            if (!summary.options().isEmpty()) {
                tradeOption.setValue(summary.options().getFirst());
                orderOption.setValue(summary.options().getFirst());
                winningOption.setValue(summary.options().getFirst());
            }
        }
        TableRefresh.update(optionTable, details.marketOptions(),
                OptionMarketView::optionNumber);
        List<OrderView> orders = new ArrayList<>();
        for (OptionMarketView option : details.marketOptions()) {
            orders.addAll(option.buyOrders());
            orders.addAll(option.sellOrders());
        }
        TableRefresh.update(orderTable, orders, OrderView::orderId);
        TableRefresh.update(executionTable, details.executionsNewestFirst(),
                ExecutionView::executionId);
        TableRefresh.update(tradeTable, details.tradesNewestFirst(),
                TradeView::tradeNumber);
        TableRefresh.update(positionTable, details.participantPositions(),
                PositionView::userName);
        showControls(true);
    }

    private void configureTables() {
        optionTable.getColumns().setAll(List.of(
                column("Option", OptionMarketView::name, 120),
                column("Shares", option -> Long.toString(option.issuedShares()), 75),
                column("Value", option -> amount(option.currentValue()), 85),
                column("Last", option -> amount(option.lastPrice()), 85),
                column("Bid", option -> amount(option.bestBid()), 85),
                column("Ask", option -> amount(option.bestAsk()), 85)));
        orderTable.getColumns().setAll(List.of(
                column("Option", order -> Integer.toString(order.optionNumber()), 65),
                column("Side", OrderView::side, 70),
                column("User", OrderView::userName, 105),
                column("Remaining", order -> Long.toString(order.remainingQuantity()), 90),
                column("Limit", order -> money(order.limitPrice()), 80),
                column("Status", OrderView::status, 120)));
        executionTable.getColumns().setAll(List.of(
                column("Option", execution -> Integer.toString(execution.optionNumber()), 65),
                column("Buyer", ExecutionView::buyerName, 110),
                column("Seller", execution -> execution.minted()
                        ? "Minted" : execution.sellerName(), 110),
                column("Quantity", execution -> Long.toString(execution.quantity()), 80),
                column("Price", execution -> money(execution.unitPrice()), 80)));
        tradeTable.getColumns().setAll(List.of(
                column("Option", TradeView::optionName, 110),
                column("Quantity", trade -> Long.toString(trade.quantity()), 85),
                column("Cost", trade -> money(trade.shareCost()), 85),
                column("Commission", trade -> money(trade.commission()), 105),
                column("Paid", trade -> money(trade.totalPaid()), 85)));
        positionTable.getColumns().setAll(List.of(
                column("User", PositionView::userName, 110),
                column("Shares", position -> Long.toString(position.totalShares()), 85),
                column("Paid", position -> money(position.totalAmountPaid()), 85),
                column("Commission", position -> money(position.totalCommissionPaid()), 105)));
        for (TableView<?> table : List.of(optionTable, orderTable, executionTable,
                tradeTable, positionTable)) {
            table.setPrefHeight(280);
        }
        dataTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        dataTabs.getTabs().addAll(new Tab("Options", optionTable),
                new Tab("Orders", orderTable), new Tab("Executions", executionTable),
                new Tab("Trades", tradeTable), new Tab("Participants", positionTable));
    }

    private void clearTables() {
        optionTable.getItems().clear();
        orderTable.getItems().clear();
        executionTable.getItems().clear();
        tradeTable.getItems().clear();
        positionTable.getItems().clear();
    }

    private void showControls(boolean hasDetails) {
        actions.setManaged(hasDetails);
        actions.setVisible(hasDetails);
        dataTabs.setManaged(hasDetails);
        dataTabs.setVisible(hasDetails);
        if (!hasDetails) {
            return;
        }
        EventView event = current.summary();
        boolean maker = userName.equals(event.marketMakerName());
        boolean active = "ACTIVE".equals(event.status());
        visible(openRow, maker && "NOT_STARTED".equals(event.status()));
        visible(purchaseRow, active && "LMSR".equals(event.tradingMethod()));
        visible(orderRow, active && "ORDER_BOOK".equals(event.tradingMethod()));
        visible(closeRow, maker && active);
        for (Button button : List.of(openButton, purchaseButton, orderButton, closeButton)) {
            button.setDisable(busy);
        }
    }

    private void purchase() {
        Long count = quantity(quantity);
        OptionView option = tradeOption.getValue();
        if (count == null || option == null) {
            return;
        }
        int eventId = selectedId;
        submit(() -> api.purchase(eventId, option.optionNumber(), count));
    }

    private void order() {
        Long count = quantity(orderQuantity);
        if (count == null || orderOption.getValue() == null) {
            return;
        }
        double limit;
        try {
            limit = Double.parseDouble(price.getText().trim());
            if (!Double.isFinite(limit) || limit <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            feedback.accept("Enter a positive limit price.", true);
            return;
        }
        int eventId = selectedId;
        int optionNumber = orderOption.getValue().optionNumber();
        String orderSide = side.getValue();
        submit(() -> api.order(eventId, optionNumber, orderSide, count, limit));
    }

    private void close() {
        OptionView winner = winningOption.getValue();
        if (winner == null) {
            feedback.accept("Choose the winning option.", true);
            return;
        }
        int eventId = selectedId;
        submit(() -> api.close(eventId, winner.optionNumber()));
    }

    private Long quantity(TextField field) {
        try {
            long value = Long.parseLong(field.getText().trim());
            if (value < 1) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            feedback.accept("Enter a positive whole quantity.", true);
            return null;
        }
    }

    private void submit(Callable<ActionResultView> request) {
        busy = true;
        showControls(current != null);
        MarketClientApplication.runAsync(request, result -> {
            busy = false;
            update(result.event());
            onAction.accept(result);
            feedback.accept("Event updated.", false);
        }, failure -> {
            busy = false;
            showControls(current != null);
            feedback.accept(MarketClientApplication.message(failure), true);
        });
    }

    private static void visible(FlowPane row, boolean value) {
        row.setManaged(value);
        row.setVisible(value);
    }

    private static Button command(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private static ComboBox<OptionView> optionChooser() {
        ComboBox<OptionView> combo = new ComboBox<>();
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(OptionView option) {
                return option == null ? "" : option.name();
            }

            @Override
            public OptionView fromString(String text) {
                return null;
            }
        });
        combo.setPrefWidth(150);
        return combo;
    }

    private static <T> TableColumn<T, String> column(
            String title, Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Objects.toString(value.apply(cell.getValue()), "")));
        column.setPrefWidth(width);
        return column;
    }

    private static String amount(Double value) {
        return value == null ? "" : money(value);
    }

    private static String money(double value) {
        return String.format(java.util.Locale.ROOT, "%,.2f", value);
    }
}
