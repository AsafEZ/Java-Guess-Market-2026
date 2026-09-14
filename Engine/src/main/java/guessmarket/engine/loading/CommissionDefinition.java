package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;

import java.util.Objects;

record CommissionDefinition(CommissionType type, int percentage) {
    CommissionDefinition {
        type = Objects.requireNonNull(type, "type");
    }
}
