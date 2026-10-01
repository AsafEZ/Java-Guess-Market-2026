package guessmarket.server;

import guessmarket.protocol.ChatMessageView;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

final class ChatRoom {
    static final String CONTEXT_ATTRIBUTE = ChatRoom.class.getName();

    private final Clock clock;
    private final List<ChatMessageView> messages = new ArrayList<>();

    ChatRoom() {
        this(Clock.systemUTC());
    }

    ChatRoom(Clock clock) {
        this.clock = clock;
    }

    synchronized ChatMessageView post(String userName, String text) {
        ChatMessageView message = new ChatMessageView(messages.size() + 1L,
                userName, text, clock.millis());
        messages.add(message);
        return message;
    }

    synchronized List<ChatMessageView> after(long id) {
        if (id >= messages.size()) {
            return List.of();
        }
        return List.copyOf(messages.subList((int) id, messages.size()));
    }
}
