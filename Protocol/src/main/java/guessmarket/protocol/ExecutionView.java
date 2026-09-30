package guessmarket.protocol;

public record ExecutionView(long executionId, int optionNumber, String buyerName,
                            String sellerName, long quantity, double unitPrice,
                            double shareCost, boolean minted) {
}
