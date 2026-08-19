package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.HashSet;
import java.util.Set;

public final class MarketDefinitionValidator {
    public void validate(MarketDefinition definition) {
        Set<Integer> ids = new HashSet<>();
        for (EventDefinition event : definition.events()) {
            if (!ids.add(event.id())) {
                throw new EngineException(
                        ErrorCode.DUPLICATE_EVENT_ID,
                        "Event id " + event.id() + " appears more than once.");
            }
            if (event.commissionPercentage() < 0 || event.commissionPercentage() > 90) {
                throw new EngineException(
                        ErrorCode.INVALID_COMMISSION,
                        "Event " + event.id() + " has commission "
                                + event.commissionPercentage() + "; expected 0 to 90.");
            }
            if (event.optionNames().size() != 2) {
                throw new EngineException(
                        ErrorCode.INVALID_OPTION_COUNT,
                        "Event " + event.id() + " must contain exactly two options.");
            }
            if (event.b() <= 0) {
                throw new EngineException(
                        ErrorCode.INVALID_LMSR_B,
                        "Event " + event.id() + " has LMSR b=" + event.b()
                                + "; b must be a positive whole number.");
            }
        }
    }
}
