package guessmarket.client;

import guessmarket.client.net.MarketApiClient;
import guessmarket.protocol.ChatMessageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

final class ChatPane extends VBox {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final MarketApiClient api;
    private final BiConsumer<String, Boolean> feedback;
    private final ObservableList<ChatMessageView> messages = FXCollections.observableArrayList();
    private final ListView<ChatMessageView> list = new ListView<>(messages);
    private final TextField input = new TextField();
    private final Button send = new Button("Send");
    private volatile long lastId;

    ChatPane(MarketApiClient api, BiConsumer<String, Boolean> feedback) {
        this.api = Objects.requireNonNull(api);
        this.feedback = Objects.requireNonNull(feedback);
        getStyleClass().add("chat-pane");
        setPadding(new Insets(16));
        setSpacing(12);

        Label title = new Label("Chat");
        title.getStyleClass().add("section-title");
        list.setPlaceholder(new Label("No messages yet"));
        list.setCellFactory(ignored -> new MessageCell());
        VBox.setVgrow(list, Priority.ALWAYS);

        input.setPromptText("Message");
        HBox.setHgrow(input, Priority.ALWAYS);
        send.getStyleClass().add("primary-button");
        send.setDisable(true);
        input.textProperty().addListener((ignored, oldValue, value) ->
                send.setDisable(value.isBlank() || input.isDisabled()));
        input.setOnAction(ignored -> send.fire());
        send.setOnAction(ignored -> sendMessage());
        HBox composer = new HBox(8, input, send);
        composer.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(title, list, composer);
    }

    long lastId() {
        return lastId;
    }

    void append(List<ChatMessageView> incoming) {
        boolean added = false;
        for (ChatMessageView message : incoming) {
            if (message.id() > lastId) {
                messages.add(message);
                lastId = message.id();
                added = true;
            }
        }
        if (added) {
            list.scrollTo(messages.size() - 1);
        }
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > 500) {
            feedback.accept("A chat message may contain at most 500 characters.", true);
            return;
        }
        input.setDisable(true);
        send.setDisable(true);
        MarketClientApplication.runAsync(() -> api.postChat(text), result -> {
            input.clear();
            input.setDisable(false);
            send.setDisable(true);
            input.requestFocus();
        }, failure -> {
            input.setDisable(false);
            send.setDisable(false);
            feedback.accept(MarketClientApplication.message(failure), true);
        });
    }

    private final class MessageCell extends ListCell<ChatMessageView> {
        private final Label label = new Label();

        private MessageCell() {
            label.setWrapText(true);
            label.prefWidthProperty().bind(list.widthProperty().subtract(38));
            label.getStyleClass().add("chat-message");
        }

        @Override
        protected void updateItem(ChatMessageView message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) {
                setGraphic(null);
            } else {
                label.setText(message.userName() + "  "
                        + TIME.format(Instant.ofEpochMilli(message.sentAtEpochMillis()))
                        + "\n" + message.text());
                setGraphic(label);
            }
        }
    }
}
