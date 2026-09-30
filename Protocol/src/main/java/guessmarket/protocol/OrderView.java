package guessmarket.protocol;

public record OrderView(long orderId, String userName, int optionNumber,
                        String side, long originalQuantity, long remainingQuantity,
                        double limitPrice, String status) {
}
