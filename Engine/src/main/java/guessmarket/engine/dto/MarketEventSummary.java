package guessmarket.engine.dto;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;
import guessmarket.engine.enums.TradingMethod;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MarketEventSummary(
        int eventId,
        String name,
        String description,
        EventStatus status,
        TradingMethod tradingMethod,
        int commissionPercentage,
        CommissionType commissionType,
        double accountBalance,
        List<OptionSummary> options,
        Optional<String> marketMakerName) {

    public MarketEventSummary {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(tradingMethod, "tradingMethod");
        Objects.requireNonNull(commissionType, "commissionType");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        marketMakerName = Objects.requireNonNull(marketMakerName, "marketMakerName");
    }
}
