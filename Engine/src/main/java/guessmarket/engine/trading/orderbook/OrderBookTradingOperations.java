package guessmarket.engine.trading.orderbook;

import guessmarket.engine.trading.TradingMechanism;
import guessmarket.engine.trading.WinningPayoutOperations;

import guessmarket.engine.enums.OrderSide;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

public interface OrderBookTradingOperations
        extends TradingMechanism, WinningPayoutOperations {
    boolean isMintAllowed();

    int getInitialInvestment();

    int getD();

    PreparedOrderSubmission prepareOrder(
            String userName,
            int optionNumber,
            OrderSide side,
            long quantity,
            double limitPrice);

    OrderSubmissionOutcome applyPreparedOrder(PreparedOrderSubmission prepared);

    default OrderSubmissionOutcome submitOrder(
            String userName,
            int optionNumber,
            OrderSide side,
            long quantity,
            double limitPrice) {
        return applyPreparedOrder(prepareOrder(
                userName, optionNumber, side, quantity, limitPrice));
    }

    List<LimitOrder> getOpenOrders(int optionNumber);

    List<OrderExecution> getExecutions();

    Set<String> getPendingParticipantNames();

    long getPendingSellQuantity(String userName, int optionNumber);

    OptionalDouble getBestBid(int optionNumber);

    OptionalDouble getBestAsk(int optionNumber);

    OptionalDouble getLastPrice(int optionNumber);

    OptionalDouble getMidPrice(int optionNumber);

    OptionalDouble getSpread(int optionNumber);

    @Override
    default double getPayoutPerWinningShare() {
        return getD();
    }
}
