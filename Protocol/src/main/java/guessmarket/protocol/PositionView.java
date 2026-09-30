package guessmarket.protocol;

import java.util.List;

public record PositionView(String userName, int eventId, String eventName,
                           String eventStatus, String tradingMethod, boolean marketMaker,
                           List<OptionPositionView> options, long totalShares,
                           double totalAmountPaid, double totalCommissionPaid,
                           List<TradeView> tradesNewestFirst,
                           Integer winningOptionNumber, String winningOptionName) {
}
