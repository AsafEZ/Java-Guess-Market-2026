package guessmarket.engine.impl;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CloseOutcome;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.PurchaseOutcome;
import guessmarket.engine.domain.SettlementOutcome;
import guessmarket.engine.trading.orderbook.OrderSubmissionOutcome;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.OrderSubmissionResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.loading.MarketSystemXmlLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class GuessMarketEngineImpl implements GuessMarketEngine {
    private final LmsrCalculator calculator = new LmsrCalculator();
    private final MarketSystemXmlLoader loader = new MarketSystemXmlLoader(calculator);
    private MarketSystem currentSystem;

    public GuessMarketEngineImpl() {
    }

    GuessMarketEngineImpl(MarketSystem currentSystem) {
        this.currentSystem = Objects.requireNonNull(currentSystem, "currentSystem");
    }

    @Override
    public synchronized LoadResult loadSystem(Path xmlPath) {
        validatePath(xmlPath);

        // Atomic load: currentSystem is changed only after every step succeeds.
        MarketSystemXmlLoader.LoadedSystem loaded = loader.load(xmlPath);
        currentSystem = loaded.system();

        return new LoadResult(
                xmlPath.toAbsolutePath().normalize(),
                loaded.system().size(),
                loaded.totalInitialSubsidy());
    }

    @Override
    public synchronized List<EventSummary> getAllEvents() {
        return requireSystem().getAllEvents().stream()
                .map(EventDtoMapper::toSummary)
                .toList();
    }

    @Override
    public synchronized List<EventSummary> getActiveEvents() {
        return requireSystem().getActiveEvents().stream()
                .map(EventDtoMapper::toSummary)
                .toList();
    }

    @Override
    public synchronized EventDetails getEventDetails(int eventId) {
        return EventDtoMapper.toDetails(requireSystem().getEvent(eventId));
    }

    @Override
    public synchronized PurchaseResult purchaseShares(
            int eventId,
            int optionNumber,
            long shareQuantity) {
        MarketEvent event = requireSystem().getEvent(eventId);
        PurchaseOutcome outcome = event.purchase(optionNumber, shareQuantity);
        EventDetails updated = EventDtoMapper.toDetails(event);
        return new PurchaseResult(
                eventId,
                outcome.optionNumber(),
                outcome.shareQuantity(),
                outcome.shareCost(),
                outcome.commission(),
                outcome.totalPaid(),
                updated);
    }

    @Override
    public synchronized CloseEventResult closeEvent(int eventId, int winningOptionNumber) {
        MarketEvent event = requireSystem().getEvent(eventId);
        CloseOutcome outcome = event.close(winningOptionNumber);
        EventDetails closed = EventDtoMapper.toDetails(event);
        return new CloseEventResult(
                eventId,
                outcome.winningOptionNumber(),
                outcome.winningOptionName(),
                outcome.grossPayout(),
                outcome.commission(),
                outcome.netPayout(),
                closed);
    }

    @Override
    public synchronized List<MarketEventSummary> getAllMarketEvents() {
        return requireSystem().getAllEvents().stream()
                .map(MarketEventDtoMapper::toSummary)
                .toList();
    }

    @Override
    public synchronized MarketEventDetails getMarketEventDetails(int eventId) {
        MarketSystem system = requireSystem();
        return MarketEventDtoMapper.toDetails(system.getEvent(eventId), system);
    }

    @Override
    public synchronized List<UserSummary> getAllUsers() {
        return requireSystem().getAllUsers().stream()
                .map(UserDtoMapper::toSummary)
                .toList();
    }

    @Override
    public synchronized UserDetails getUserDetails(String userName) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(userName);
        return UserDtoMapper.toDetails(system.getUser(normalizedName), system);
    }

    @Override
    public synchronized MarketEventDetails openEvent(
            int eventId,
            String actingUserName) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(actingUserName);
        system.openEvent(eventId, normalizedName);
        return MarketEventDtoMapper.toDetails(system.getEvent(eventId), system);
    }

    @Override
    public synchronized UserPurchaseResult purchaseShares(
            int eventId,
            String buyerName,
            int optionNumber,
            long shareQuantity) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(buyerName);
        PurchaseOutcome outcome = system.purchaseShares(
                normalizedName, eventId, optionNumber, shareQuantity);
        return PurchaseDtoMapper.toResult(
                normalizedName, eventId, outcome, system);
    }

    @Override
    public synchronized SettlementResult closeEvent(
            int eventId,
            String actingUserName,
            int winningOptionNumber) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(actingUserName);
        SettlementOutcome outcome = system.closeEvent(
                eventId, normalizedName, winningOptionNumber);
        return SettlementDtoMapper.toResult(outcome, system);
    }

    @Override
    public synchronized OrderSubmissionResult submitOrder(
            int eventId,
            String userName,
            int optionNumber,
            OrderSide side,
            long quantity,
            double limitPrice) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(userName);
        OrderSubmissionOutcome outcome = system.submitOrder(
                normalizedName,
                eventId,
                optionNumber,
                side,
                quantity,
                limitPrice);
        return OrderBookDtoMapper.toSubmissionResult(
                outcome,
                system.getEvent(eventId),
                system.getUser(normalizedName),
                system);
    }

    @Override
    public synchronized boolean isSystemLoaded() {
        return currentSystem != null;
    }

    private MarketSystem requireSystem() {
        if (currentSystem == null) {
            throw new EngineException(
                    ErrorCode.NO_SYSTEM_LOADED,
                    "No valid XML system is currently loaded.");
        }
        return currentSystem;
    }

    private static String normalizeUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new EngineException(
                    ErrorCode.INVALID_USER_NAME,
                    "User name cannot be null or blank.");
        }
        return userName.trim();
    }

    private static void validatePath(Path xmlPath) {
        if (xmlPath == null) {
            throw new EngineException(ErrorCode.INVALID_FILE_PATH, "The XML path cannot be null.");
        }
        String fileName = xmlPath.getFileName() == null ? "" : xmlPath.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new EngineException(
                    ErrorCode.NOT_XML_FILE,
                    "The selected file must have an .xml extension.");
        }
        if (!Files.isRegularFile(xmlPath)) {
            throw new EngineException(
                    ErrorCode.FILE_NOT_FOUND,
                    "The XML file does not exist or is not a regular file: " + xmlPath + ".");
        }
    }
}
