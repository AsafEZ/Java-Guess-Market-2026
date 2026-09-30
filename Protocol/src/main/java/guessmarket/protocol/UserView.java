package guessmarket.protocol;

public record UserView(
        String name,
        double balance,
        String status,
        boolean marketMaker) {
}
