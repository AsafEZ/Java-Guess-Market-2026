package guessmarket.engine.dto;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;

import java.util.List;
import java.util.Objects;

public record EventDetails(
        int eventId,
        String name,
        String description,
        int commissionPercentage,
        CommissionType commissionType,
        EventStatus status,
        List<OptionDetails> options,
        double accountBalance,
        double totalCommissionCollected,
        List<TradeDetails> tradesNewestFirst,
        Integer winningOptionNumber,
        String winningOptionName) {

    public EventDetails {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(commissionType, "commissionType");
        Objects.requireNonNull(status, "status");
        options = List.copyOf(options);
        tradesNewestFirst = List.copyOf(tradesNewestFirst);
    }}