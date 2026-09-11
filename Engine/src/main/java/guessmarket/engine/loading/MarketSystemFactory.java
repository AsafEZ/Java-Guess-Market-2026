package guessmarket.engine.loading;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CommissionPolicy;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;

import java.util.ArrayList;
import java.util.List;

public final class MarketSystemFactory {
    private final LmsrCalculator calculator;

    public MarketSystemFactory(LmsrCalculator calculator) {
        this.calculator = calculator;
    }

    public MarketSystem create(MarketDefinition definition) {
        MarketSystem system = new MarketSystem();

        for (EventDefinition event : definition.events()) {
            List<MarketOption> options = new ArrayList<>();

            for (int index = 0;
                 index < event.optionNames().size();
                 index++) {

                options.add(new MarketOption(
                        index + 1,
                        event.optionNames().get(index)
                ));
            }

            LmsrTradingMechanism lmsrTradingMechanism = new LmsrTradingMechanism(event.b(), calculator);

            MarketEvent marketEvent = new MarketEvent(
                    event.id(),
                    event.name(),
                    event.description(),
                    options,
                    new CommissionPolicy(
                            event.commissionPercentage(),
                            event.commissionType()
                    ),
                    lmsrTradingMechanism,
                    lmsrTradingMechanism.calculateInitialSubsidy()
            );

            system.addEvent(marketEvent);
        }

        return system;
    }
}
