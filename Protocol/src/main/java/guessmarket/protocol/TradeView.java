package guessmarket.protocol;

public record TradeView(long tradeNumber, int optionNumber, String optionName,
                        long quantity, double shareCost, double commission,
                        double totalPaid) {
}
