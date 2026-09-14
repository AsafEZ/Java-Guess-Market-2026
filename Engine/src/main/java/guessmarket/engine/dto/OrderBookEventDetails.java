package guessmarket.engine.dto;

import guessmarket.engine.enums.TradingMethod;

import java.util.List;
import java.util.Objects;

public record OrderBookEventDetails(
        boolean mintAllowed,
        int initialInvestment,
        int d,
        List<OrderBookOptionDetails> options,
        List<OrderExecutionDetails> executionsNewestFirst)
        implements TradingMechanismDetails {

    public OrderBookEventDetails {
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        executionsNewestFirst = List.copyOf(
                Objects.requireNonNull(executionsNewestFirst, "executionsNewestFirst"));
    }

    @Override
    public TradingMethod tradingMethod() {
        return TradingMethod.ORDER_BOOK;
    }
}
