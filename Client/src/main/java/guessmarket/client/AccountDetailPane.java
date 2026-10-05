package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.AccountActivityView;
import guessmarket.protocol.OptionPositionView;
import guessmarket.protocol.PositionView;
import guessmarket.protocol.TradeView;
import guessmarket.protocol.UserDetailsView;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class AccountDetailPane extends ScrollPane {
    private final MarketApiClient api;
    private final BiConsumer<String, Boolean> feedback;
    private final Label balance = new Label("Balance: 0.00");
    private final Label positionSummary = new Label("Select an event position");
    private final TableView<PositionView> positions = new TableView<>();
    private final TableView<OptionPositionView> options = new TableView<>();
    private final TableView<TradeView> trades = new TableView<>();
    private final TableView<AccountActivityView> history = new TableView<>();
    private boolean replacingPositions;

    AccountDetailPane(MarketApiClient api, BiConsumer<String, Boolean> feedback) {
        this.api = Objects.requireNonNull(api);
        this.feedback = Objects.requireNonNull(feedback);
        getStyleClass().add("detail-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        Label title = new Label("My account");
        title.getStyleClass().add("section-title");
        balance.getStyleClass().add("balance-label");
        TextField amount = new TextField();
        amount.setPromptText("Amount");
        amount.setMaxWidth(180);
        Button deposit = new Button("Add funds");
        deposit.getStyleClass().add("primary-button");
        deposit.setOnAction(ignored -> deposit(amount, deposit));
        amount.setOnAction(ignored -> deposit.fire());
        HBox depositRow = new HBox(8, amount, deposit);
        depositRow.setAlignment(Pos.CENTER_LEFT);

        configureTables();
        VBox positionView = new VBox(10, positions, positionSummary, options, trades);
        VBox historyView = new VBox(10, history);
        TabPane tabs = new TabPane(new Tab("Positions", positionView),
                new Tab("History", historyView));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox body = new VBox(14, title, balance, depositRow, tabs);
        body.setPadding(new Insets(18));
        body.getStyleClass().add("detail-pane");
        setContent(body);
    }

    void update(UserDetailsView account, List<AccountActivityView> activity) {
        updateBalance(account.balance());
        replacingPositions = true;
        try {
            if (TableRefresh.update(positions, account.positions(), PositionView::eventId)) {
                showPosition(positions.getSelectionModel().getSelectedItem());
            }
        } finally {
            replacingPositions = false;
        }
        TableRefresh.update(history, activity, AccountActivityView::id);
    }

    void updateBalance(double value) {
        String next = "Balance: " + money(value);
        if (!balance.getText().equals(next)) {
            balance.setText(next);
        }
    }

    private void deposit(TextField amount, Button button) {
        double value;
        try {
            value = Double.parseDouble(amount.getText().trim());
            if (!Double.isFinite(value) || value <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            feedback.accept("Enter a positive amount.", true);
            return;
        }
        button.setDisable(true);
        MarketClientApplication.runAsync(() -> api.deposit(value), user -> {
            button.setDisable(false);
            amount.clear();
            updateBalance(user.balance());
            feedback.accept("Funds added.", false);
        }, failure -> {
            button.setDisable(false);
            feedback.accept(MarketClientApplication.message(failure), true);
        });
    }

    private void configureTables() {
        positions.getColumns().setAll(List.of(
                column("Event", PositionView::eventName, 160),
                column("Status", PositionView::eventStatus, 100),
                column("Role", position -> position.marketMaker() ? "Maker" : "Trader", 75),
                column("Shares", position -> Long.toString(position.totalShares()), 75),
                column("Paid", position -> money(position.totalAmountPaid()), 85)));
        positions.setPlaceholder(new Label("No event positions"));
        positions.setPrefHeight(210);
        positions.getSelectionModel().selectedItemProperty().addListener(
                (ignored, oldValue, selected) -> {
                    if (!replacingPositions) {
                        showPosition(selected);
                    }
                });
        positionSummary.setWrapText(true);

        options.getColumns().setAll(List.of(
                column("Option", OptionPositionView::name, 120),
                column("Shares", option -> Long.toString(option.shares()), 75),
                column("Paid", option -> money(option.amountPaid()), 85),
                column("Commission", option -> money(option.commissionPaid()), 100),
                column("Winner", option -> option.winningOption() ? "Yes" : "", 75)));
        options.setPlaceholder(new Label("Select an event position"));
        options.setPrefHeight(170);

        trades.getColumns().setAll(List.of(
                column("Option", TradeView::optionName, 120),
                column("Quantity", trade -> Long.toString(trade.quantity()), 75),
                column("Cost", trade -> money(trade.shareCost()), 85),
                column("Commission", trade -> money(trade.commission()), 100),
                column("Paid", trade -> money(trade.totalPaid()), 85)));
        trades.setPlaceholder(new Label("No trades for this event"));
        trades.setPrefHeight(170);

        history.getColumns().setAll(List.of(
                column("Time (UTC)", AccountActivityView::occurredAt, 175),
                column("Action", AccountActivityView::action, 130),
                column("Event", AccountActivityView::eventName, 140),
                column("Change", row -> money(row.amount()), 90),
                column("Commission", row -> money(row.commission()), 95),
                column("Balance", row -> money(row.balanceAfter()), 90)));
        history.setPlaceholder(new Label("No account activity yet"));
        history.setPrefHeight(470);
    }

    private void showPosition(PositionView position) {
        if (position == null) {
            positionSummary.setText("Select an event position");
            options.getItems().clear();
            trades.getItems().clear();
            return;
        }
        positionSummary.setText(position.eventName() + "  |  "
                + position.tradingMethod().replace('_', ' ') + "  |  "
                + position.eventStatus().replace('_', ' ')
                + (position.winningOptionName() == null ? ""
                : "  |  Winner: " + position.winningOptionName()));
        TableRefresh.update(options, position.options(), OptionPositionView::optionNumber);
        TableRefresh.update(trades, position.tradesNewestFirst(), TradeView::tradeNumber);
    }

    private static <T> TableColumn<T, String> column(
            String title, Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                Objects.toString(value.apply(cell.getValue()), "")));
        column.setPrefWidth(width);
        return column;
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}
