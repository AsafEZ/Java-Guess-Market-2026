package guessmarket.engine.dto;

import guessmarket.engine.enums.TradingMethod;

public sealed interface TradingMechanismDetails permits LmsrEventDetails {
    TradingMethod tradingMethod();
}
