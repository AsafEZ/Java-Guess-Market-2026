package guessmarket.engine.api;

import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;

import java.nio.file.Path;
import java.util.List;


public interface GuessMarketEngine {
    LoadResult loadSystem(Path xmlPath);

    List<EventSummary> getAllEvents();

    List<EventSummary> getActiveEvents();

    EventDetails getEventDetails(int eventId);

    PurchaseResult purchaseShares(int eventId, int optionNumber, long shareQuantity);

    CloseEventResult closeEvent(int eventId, int winningOptionNumber);

    List<MarketEventSummary> getAllMarketEvents();

    MarketEventDetails getMarketEventDetails(int eventId);

    List<UserSummary> getAllUsers();

    UserDetails getUserDetails(String userName);

    MarketEventDetails openEvent(int eventId, String actingUserName);

    UserPurchaseResult purchaseShares(
            int eventId,
            String buyerName,
            int optionNumber,
            long shareQuantity);

    SettlementResult closeEvent(
            int eventId,
            String actingUserName,
            int winningOptionNumber);

    boolean isSystemLoaded();
}
