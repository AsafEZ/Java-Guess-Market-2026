package guessmarket.protocol;

import java.util.List;

public record EventView(
        int eventId,
        String name,
        String description,
        String status,
        String tradingMethod,
        int commissionPercentage,
        String commissionType,
        double accountBalance,
        String marketMakerName,
        List<OptionView> options) {
}
