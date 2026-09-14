package guessmarket.engine.loading;

import java.util.List;
import java.util.Objects;

record Assignment2Definition(
        List<UserDefinition> users,
        List<Assignment2EventDefinition> events) {

    Assignment2Definition {
        users = List.copyOf(Objects.requireNonNull(users, "users"));
        events = List.copyOf(Objects.requireNonNull(events, "events"));
    }
}
