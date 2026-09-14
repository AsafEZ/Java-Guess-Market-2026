package guessmarket.engine.loading;

import java.util.List;
import java.util.Objects;

record UserDefinition(
        String name,
        int initialCash,
        List<Integer> marketMakerEventIds) {

    UserDefinition {
        marketMakerEventIds = List.copyOf(
                Objects.requireNonNull(marketMakerEventIds, "marketMakerEventIds"));
    }
}
