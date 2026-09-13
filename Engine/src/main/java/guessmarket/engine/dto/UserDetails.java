package guessmarket.engine.dto;

import guessmarket.engine.enums.UserStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record UserDetails(
        String name,
        double balance,
        UserStatus status,
        Set<Integer> marketMakerEventIds,
        List<PositionDetails> positions) {

    public UserDetails {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        marketMakerEventIds = Set.copyOf(
                Objects.requireNonNull(marketMakerEventIds, "marketMakerEventIds"));
        positions = List.copyOf(Objects.requireNonNull(positions, "positions"));
    }
}
