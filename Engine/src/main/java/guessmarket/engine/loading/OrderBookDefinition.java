package guessmarket.engine.loading;

record OrderBookDefinition(
        boolean allowMint,
        int initial,
        int d) implements TradingMechanismDefinition {
}
