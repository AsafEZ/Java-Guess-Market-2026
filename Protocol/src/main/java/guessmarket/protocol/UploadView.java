package guessmarket.protocol;

import java.util.List;

public record UploadView(
        int uploadedEventCount,
        int totalEventCount,
        List<String> eventNames) {
}
