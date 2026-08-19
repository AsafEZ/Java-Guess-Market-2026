package guessmarket.engine.dto;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;

import java.util.List;
import java.util.Objects;

public record EventSummary(
        int eventId,
        String name,
        String description,
        int commissionPercentage,
        CommissionType commissionType,
        List<OptionSummary> options,
        EventStatus status) {

    public EventSummary {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(commissionType, "commissionType");
        options = List.copyOf(options);
        Objects.requireNonNull(status, "status");
    }}