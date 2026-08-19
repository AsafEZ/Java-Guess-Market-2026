package guessmarket.engine.loading;

import java.util.List;

public record MarketDefinition(List<EventDefinition> events) {
    public MarketDefinition {
        events = List.copyOf(events);
    }
}
