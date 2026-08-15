package guessmarket.engine.api;

import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.PurchaseResult;

import java.nio.file.Path;
import java.util.List;


public interface GuessMarketEngine {
    LoadResult loadSystem(Path xmlPath);

    List<EventSummary> getAllEvents();

    List<EventSummary> getActiveEvents();

    EventDetails getEventDetails(int eventId);

    PurchaseResult purchaseShares(int eventId, int optionNumber, long shareQuantity);

    CloseEventResult closeEvent(int eventId, int winningOptionNumber);

    boolean isSystemLoaded();
}
