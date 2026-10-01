package guessmarket.protocol;

public record ChatMessageView(long id, String userName, String text, long sentAtEpochMillis) {
}
