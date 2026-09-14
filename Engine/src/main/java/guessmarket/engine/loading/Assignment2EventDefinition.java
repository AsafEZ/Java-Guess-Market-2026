package guessmarket.engine.loading;

import java.util.List;
import java.util.Objects;

record Assignment2EventDefinition(
        int id,
        String name,
        String description,
        CommissionDefinition commission,
        List<OptionDefinition> options,
        TradingMechanismDefinition tradingMechanism) {

    Assignment2EventDefinition {
        commission = Objects.requireNonNull(commission, "commission");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        tradingMechanism = Objects.requireNonNull(
                tradingMechanism,
                "tradingMechanism");
    }
}
