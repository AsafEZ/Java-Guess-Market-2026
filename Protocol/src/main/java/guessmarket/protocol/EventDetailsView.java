package guessmarket.protocol;

import java.util.List;

public record EventDetailsView(EventView summary, double totalCommissionCollected,
                               Integer winningOptionNumber, String winningOptionName,
                               Integer lmsrB, Boolean mintAllowed,
                               Integer initialInvestment, Integer d,
                               List<OptionMarketView> marketOptions,
                               List<TradeView> tradesNewestFirst,
                               List<ExecutionView> executionsNewestFirst,
                               List<PositionView> participantPositions) {
}
