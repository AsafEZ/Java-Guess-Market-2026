package guessmarket.server;

import guessmarket.engine.dto.EventUploadResult;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.UserSummary;
import guessmarket.protocol.EventView;
import guessmarket.protocol.OptionView;
import guessmarket.protocol.UploadView;
import guessmarket.protocol.UserView;

import java.util.Set;

final class ViewMapper {
    private ViewMapper() {
    }

    static EventView event(MarketEventSummary source) {
        return new EventView(
                source.eventId(), source.name(), source.description(),
                source.status().name(), source.tradingMethod().name(),
                source.commissionPercentage(), source.commissionType().name(),
                source.accountBalance(), source.marketMakerName().orElse(null),
                source.options().stream()
                        .map(option -> new OptionView(option.optionNumber(), option.name()))
                        .toList());
    }

    static UserView user(UserSummary source, Set<String> marketMakerNames) {
        return new UserView(source.name(), source.balance(), source.status().name(),
                marketMakerNames.contains(source.name()));
    }

    static UploadView upload(EventUploadResult source) {
        return new UploadView(source.uploadedEventCount(),
                source.totalEventCount(), source.eventNames());
    }
}
