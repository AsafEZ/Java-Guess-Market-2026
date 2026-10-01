package guessmarket.server;

import guessmarket.protocol.ChatMessageView;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatRoomTest {
    @Test
    void everyUserSeesServerOrderedMessagesAndCanFetchOnlyNewOnes() {
        ChatRoom room = new ChatRoom(Clock.fixed(
                Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC));
        ChatMessageView first = room.post("Alice", "Hello");
        ChatMessageView second = room.post("Bob", "Hi");

        assertEquals(1, first.id());
        assertEquals(2, second.id());
        assertEquals("Alice", first.userName());
        assertEquals("Bob", second.userName());
        assertEquals(List.of(first, second), room.after(0));
        assertEquals(List.of(second), room.after(1));
        assertEquals(List.of(), room.after(2));
        assertEquals(List.of(), room.after(Long.MAX_VALUE));
        assertEquals(Instant.parse("2026-10-01T12:00:00Z").toEpochMilli(),
                first.sentAtEpochMillis());
    }
}
