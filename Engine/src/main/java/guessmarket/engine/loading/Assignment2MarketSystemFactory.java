package guessmarket.engine.loading;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CommissionPolicy;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketOption;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.User;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.lmsr.LmsrTradingMechanism;
import guessmarket.engine.trading.orderbook.OrderBookTradingMechanism;

import java.util.ArrayList;
import java.util.List;

final class Assignment2MarketSystemFactory {
    private final LmsrCalculator calculator;

    Assignment2MarketSystemFactory(LmsrCalculator calculator) {
        this.calculator = calculator;
    }

    CreationResult create(Assignment2Definition definition) {
        MarketSystem system = new MarketSystem();
        for (UserDefinition user : definition.users()) {
            system.addUser(new User(user.name(), user.initialCash()));
        }

        double totalInitialSubsidy = 0.0;
        for (Assignment2EventDefinition event : definition.events()) {
            TradingMechanism mechanism = createMechanism(event.tradingMechanism());
            if (mechanism instanceof LmsrTradingMechanism lmsr) {
                totalInitialSubsidy = addFinite(
                        totalInitialSubsidy,
                        lmsr.calculateInitialSubsidy());
            }
            system.addEvent(MarketEvent.createNotStartedEvent(
                    event.id(),
                    event.name().trim(),
                    event.description().trim(),
                    createOptions(event.options()),
                    new CommissionPolicy(
                            event.commission().percentage(),
                            event.commission().type()),
                    mechanism));
        }

        for (UserDefinition user : definition.users()) {
            for (int eventId : user.marketMakerEventIds()) {
                system.assignMarketMaker(eventId, user.name());
            }
        }
        return new CreationResult(system, totalInitialSubsidy);
    }

    private TradingMechanism createMechanism(
            TradingMechanismDefinition definition) {
        if (definition instanceof LmsrDefinition lmsr) {
            return new LmsrTradingMechanism(lmsr.b(), calculator);
        }
        if (definition instanceof OrderBookDefinition orderBook) {
            return new OrderBookTradingMechanism(
                    orderBook.allowMint(),
                    orderBook.initial(),
                    orderBook.d());
        }
        throw new EngineException(
                ErrorCode.WRONG_TRADING_METHOD,
                "Unsupported Assignment 2 trading mechanism definition.");
    }

    private static List<MarketOption> createOptions(
            List<OptionDefinition> definitions) {
        List<MarketOption> options = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++) {
            options.add(new MarketOption(
                    index + 1,
                    definitions.get(index).name().trim()));
        }
        return List.copyOf(options);
    }

    private static double addFinite(double first, double second) {
        double result = first + second;
        if (!Double.isFinite(result)) {
            throw new EngineException(
                    ErrorCode.ARITHMETIC_OVERFLOW,
                    "The total LMSR initial subsidy exceeds the supported range.");
        }
        return result;
    }

    record CreationResult(MarketSystem system, double totalInitialSubsidy) {
    }
}
