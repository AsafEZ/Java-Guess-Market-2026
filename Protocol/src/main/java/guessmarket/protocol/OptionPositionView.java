package guessmarket.protocol;

public record OptionPositionView(int optionNumber, String name, long shares,
                                 double amountPaid, double commissionPaid,
                                 boolean winningOption) {
}
