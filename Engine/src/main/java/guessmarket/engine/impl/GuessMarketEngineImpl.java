package guessmarket.engine.impl;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CloseOutcome;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.PurchaseOutcome;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.MarketDefinition;
import guessmarket.engine.loading.MarketDefinitionValidator;
import guessmarket.engine.loading.MarketSystemFactory;
import guessmarket.engine.loading.XmlMarketLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class GuessMarketEngineImpl implements GuessMarketEngine {
    private final LmsrCalculator calculator = new LmsrCalculator();
    private final XmlMarketLoader loader = new XmlMarketLoader();
    private final MarketDefinitionValidator validator = new MarketDefinitionValidator();
    private final MarketSystemFactory systemFactory = new MarketSystemFactory(calculator);
    private MarketSystem currentSystem;

    @Override
    public synchronized LoadResult loadSystem(Path xmlPath) {
        validatePath(xmlPath);

        // Atomic load: currentSystem is changed only after every step succeeds.
        MarketDefinition definition = loader.load(xmlPath);
        validator.validate(definition);
        MarketSystem candidate = systemFactory.create(definition);
        currentSystem = candidate;

        return new LoadResult(
                xmlPath.toAbsolutePath().normalize(),
                candidate.size(),
                candidate.totalInitialSubsidy());
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
        return EventDtoMapper.toDetails(requireSystem().getEvent(eventId), calculator);
    }

    @Override
    public synchronized PurchaseResult purchaseShares(
            int eventId,
            int optionNumber,
            long shareQuantity) {
        MarketEvent event = requireSystem().getEvent(eventId);
        PurchaseOutcome outcome = event.purchase(optionNumber, shareQuantity);
        EventDetails updated = EventDtoMapper.toDetails(event, calculator);
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
        EventDetails closed = EventDtoMapper.toDetails(event, calculator);
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
