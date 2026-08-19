package guessmarket.engine.dto;


import java.util.Objects;

public record TradeDetails(
        long tradeNumber,
        int optionNumber,
        String optionName,
        long shareQuantity,
        double shareCost,
        double commission,
        double totalPaid) {

    public TradeDetails {
        Objects.requireNonNull(optionName, "optionName");
    }
}
