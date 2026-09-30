package guessmarket.engine.impl;

import guessmarket.engine.api.Assignment3Engine;
import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.CloseOutcome;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.domain.MarketSystem;
import guessmarket.engine.domain.User;
import guessmarket.engine.domain.PurchaseOutcome;
import guessmarket.engine.domain.SettlementOutcome;
import guessmarket.engine.trading.orderbook.OrderSubmissionOutcome;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.EventUploadResult;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.MarketEventDetails;
import guessmarket.engine.dto.MarketEventSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.OrderSubmissionResult;
import guessmarket.engine.dto.SettlementResult;
import guessmarket.engine.dto.UserDetails;
import guessmarket.engine.dto.UserPurchaseResult;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.dto.AccountActivityDetails;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.loading.MarketSystemXmlLoader;
import guessmarket.engine.loading.Assignment3EventLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.time.Instant;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.trading.orderbook.OrderExecution;

public final class GuessMarketEngineImpl implements Assignment3Engine {
    private final LmsrCalculator calculator = new LmsrCalculator();
    private final MarketSystemXmlLoader loader = new MarketSystemXmlLoader(calculator);
    private final Assignment3EventLoader assignment3Loader = new Assignment3EventLoader(calculator);
    private final MarketSystem registeredUsers = new MarketSystem();
    private MarketSystem currentSystem;
    private final Map<String, List<AccountActivityDetails>> accountHistory =
            new LinkedHashMap<>();

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
        for (User user : registeredUsers.getAllUsers()) {
            loaded.system().addUser(new User(user.getName()));
        }
        currentSystem = loaded.system();
        accountHistory.clear();

        return new LoadResult(
                xmlPath.toAbsolutePath().normalize(),
                loaded.system().size(),
                loaded.totalInitialSubsidy());
    }

    @Override
    public synchronized EventUploadResult uploadEvents(
            InputStream xmlStream, String uploaderName) {
        String normalizedName = normalizeUserName(uploaderName);
        MarketSystem source = currentSystem != null ? currentSystem : registeredUsers;
        source.getUser(normalizedName);

        long nextId = source.getAllEvents().stream()
                .mapToLong(MarketEvent::getId)
                .max().orElse(0L) + 1L;
        if (nextId > Integer.MAX_VALUE) {
            throw new EngineException(ErrorCode.ARITHMETIC_OVERFLOW,
                    "There are no event identifiers left for this upload.");
        }
        List<MarketEvent> uploaded = assignment3Loader.load(xmlStream, (int) nextId);
        Set<String> names = new HashSet<>();
        for (MarketEvent existing : source.getAllEvents()) {
            names.add(existing.getName());
        }
        for (MarketEvent event : uploaded) {
            if (!names.add(event.getName())) {
                throw new EngineException(ErrorCode.DUPLICATE_EVENT_NAME,
                        "Duplicate event name: " + event.getName() + ".");
            }
        }

        MarketSystem candidate = new MarketSystem();
        for (User user : source.getAllUsers()) {
            candidate.addUser(user);
        }
        for (MarketEvent event : source.getAllEvents()) {
            candidate.addEvent(event);
        }
        for (MarketEvent event : uploaded) {
            candidate.addEvent(event);
            candidate.assignMarketMaker(event.getId(), normalizedName);
        }
        currentSystem = candidate;
        return new EventUploadResult(uploaded.size(), candidate.size(),
                uploaded.stream().map(MarketEvent::getName).toList());
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
        return userSystem().getAllUsers().stream()
                .map(UserDtoMapper::toSummary)
                .toList();
    }

    @Override
    public synchronized UserSummary registerUser(String userName) {
        String normalizedName = normalizeUserName(userName);
        if (currentSystem != null && currentSystem.getAllUsers().stream()
                .anyMatch(user -> user.getName().equals(normalizedName))) {
            throw new EngineException(ErrorCode.DUPLICATE_USER_NAME,
                    "Duplicate user name: " + normalizedName + ".");
        }
        User user = new User(normalizedName);
        registeredUsers.addUser(user);
        if (currentSystem != null) {
            currentSystem.addUser(user);
        }
        accountHistory.put(user.getName(), new ArrayList<>());
        return UserDtoMapper.toSummary(user);
    }

    @Override
    public synchronized UserDetails getUserDetails(String userName) {
        MarketSystem system = userSystem();
        String normalizedName = normalizeUserName(userName);
        return UserDtoMapper.toDetails(system.getUser(normalizedName), system);
    }

    @Override
    public synchronized UserSummary creditAccount(String userName, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new EngineException(ErrorCode.INVALID_CREDIT_AMOUNT,
                    "Credit amount must be a positive finite number.");
        }
        User user = userSystem().getUser(normalizeUserName(userName));
        user.credit(amount);
        appendActivity(user, "DEPOSIT", null, null, amount, 0.0);
        return UserDtoMapper.toSummary(user);
    }

    @Override
    public synchronized List<AccountActivityDetails> getAccountHistory(String userName) {
        String normalizedName = normalizeUserName(userName);
        userSystem().getUser(normalizedName);
        return List.copyOf(accountHistory.getOrDefault(normalizedName, List.of()));
    }

    @Override
    public synchronized MarketEventDetails openEvent(
            int eventId,
            String actingUserName) {
        MarketSystem system = requireSystem();
        String normalizedName = normalizeUserName(actingUserName);
        Map<String, Double> before = balances(system);
        system.openEvent(eventId, normalizedName);
        recordAction(system, before, "OPEN_EVENT", eventId, normalizedName, Map.of());
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
        Map<String, Double> before = balances(system);
        PurchaseOutcome outcome = system.purchaseShares(
                normalizedName, eventId, optionNumber, shareQuantity);
        String maker = system.getEvent(eventId).getMarketMakerName();
        Map<String, Double> commissions = new LinkedHashMap<>();
        if (!normalizedName.equals(maker) && outcome.commission() > 0.0) {
            commissions.put(normalizedName, -outcome.commission());
            commissions.put(maker, outcome.commission());
        }
        recordAction(system, before, "PURCHASE", eventId, normalizedName, commissions);
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
        Map<String, Double> before = balances(system);
        SettlementOutcome outcome = system.closeEvent(
                eventId, normalizedName, winningOptionNumber);
        Map<String, Double> commissions = new LinkedHashMap<>();
        for (var settlement : outcome.userSettlements()) {
            if (settlement.closingCommission() > 0.0) {
                commissions.merge(settlement.userName(),
                        -settlement.closingCommission(), Double::sum);
            }
        }
        if (outcome.totalClosingCommission() > 0.0) {
            commissions.merge(outcome.marketMakerName(),
                    outcome.totalClosingCommission(), Double::sum);
        }
        recordAction(system, before, "CLOSE_EVENT", eventId, normalizedName, commissions);
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
        Map<String, Double> before = balances(system);
        OrderSubmissionOutcome outcome = system.submitOrder(
                normalizedName,
                eventId,
                optionNumber,
                side,
                quantity,
                limitPrice);
        Map<String, Double> commissions = new LinkedHashMap<>();
        MarketEvent event = system.getEvent(eventId);
        String maker = event.getMarketMakerName();
        if (event.getCommissionPolicy().type() == CommissionType.ON_PURCHASE) {
            for (OrderExecution execution : outcome.executions()) {
                if (!execution.buyerName().equals(maker)) {
                    double commission = event.getCommissionPolicy()
                            .calculate(execution.shareCost());
                    if (commission > 0.0) {
                        commissions.merge(execution.buyerName(), -commission, Double::sum);
                        commissions.merge(maker, commission, Double::sum);
                    }
                }
            }
        }
        recordAction(system, before, "ORDER", eventId, normalizedName, commissions);
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

    private MarketSystem userSystem() {
        return currentSystem != null ? currentSystem
                : registeredUsers.getAllUsers().isEmpty() ? requireSystem() : registeredUsers;
    }

    private static Map<String, Double> balances(MarketSystem system) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (User user : system.getAllUsers()) {
            result.put(user.getName(), user.getBalance());
        }
        return result;
    }

    private void recordAction(MarketSystem system, Map<String, Double> before,
                              String action, int eventId, String actor,
                              Map<String, Double> commissions) {
        String eventName = system.getEvent(eventId).getName();
        for (User user : system.getAllUsers()) {
            double amount = user.getBalance() - before.get(user.getName());
            if (amount != 0.0 || user.getName().equals(actor)) {
                appendActivity(user, action, eventId, eventName, amount,
                        commissions.getOrDefault(user.getName(), 0.0));
            }
        }
    }

    private void appendActivity(User user, String action, Integer eventId,
                                String eventName, double amount, double commission) {
        List<AccountActivityDetails> entries = accountHistory.computeIfAbsent(
                user.getName(), ignored -> new ArrayList<>());
        entries.add(new AccountActivityDetails(entries.size() + 1L,
                Instant.now().toString(), action, eventId, eventName,
                amount, commission, user.getBalance()));
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
