package guessmarket.engine.dto;

import guessmarket.engine.enums.TradingMethod;

import java.util.List;
import java.util.Objects;

public record LmsrEventDetails(
        int b,
        List<OptionDetails> options,
        List<TradeDetails> tradesNewestFirst) implements TradingMechanismDetails {

    public LmsrEventDetails {
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        tradesNewestFirst = List.copyOf(
                Objects.requireNonNull(tradesNewestFirst, "tradesNewestFirst"));
    }

    @Override
    public TradingMethod tradingMethod() {
        return TradingMethod.LMSR;
    }
}
