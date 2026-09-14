package guessmarket.engine.loading;

sealed interface TradingMechanismDefinition
        permits LmsrDefinition, OrderBookDefinition {
}
