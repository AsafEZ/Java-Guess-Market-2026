package guessmarket.engine.dto;

import java.util.List;
import java.util.Objects;

public record EventUploadResult(
        int uploadedEventCount,
        int totalEventCount,
        List<String> eventNames) {
    public EventUploadResult {
        eventNames = List.copyOf(Objects.requireNonNull(eventNames, "eventNames"));
    }
}
