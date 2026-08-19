package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;

import java.util.List;

public record EventDefinition(
        int id,
        String name,
        String description,
        int commissionPercentage,
        CommissionType commissionType,
        List<String> optionNames,
        int b) {

    public EventDefinition {
        optionNames = List.copyOf(optionNames);
    }
}