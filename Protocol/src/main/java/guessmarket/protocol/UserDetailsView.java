package guessmarket.protocol;

import java.util.List;

public record UserDetailsView(String name, double balance, String status,
                              List<Integer> marketMakerEventIds,
                              List<PositionView> positions) {
}
