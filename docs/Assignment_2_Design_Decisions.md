# Assignment 2 Design Decisions

## Stage 1: LMSR Encapsulation

- Status: Completed.
- Goal: Encapsulate LMSR-specific calculations and configuration behind the LMSR mechanism family contract while preserving Assignment 1 behavior.
- Rationale: Domain and mapping code should depend on trading contracts rather than the concrete LMSR implementation or its calculator.
- Design decision: Keep `TradingMechanism` minimal and expose LMSR-specific behavior through `LmsrTradingOperations`, including `getB` because `b` is an LMSR configuration parameter.
- Implementation result: `MarketEvent` depends on `LmsrTradingOperations`; direct `purchaseCost`, `optionValue`, and `initialSubsidy` calls are contained in `LmsrTradingMechanism`; `LmsrCalculator` is supplied only through the mechanism-construction dependency-injection path; no placeholder trading implementations remain.
- Test result: `mvn compile` passed; `mvn test` passed with 0 JUnit tests discovered; `EngineSmokeTest` passed with assertions enabled.
- Completion commit ID: `48bc446ec6336b4f97c8c7f8fc08386c71da47b0`

## LMSR Encapsulation - Subtask 1: Initial Subsidy Delegation

- Goal: Delegate LMSR initial subsidy calculation through the same LMSR trading mechanism instance assigned to each event.
- Rationale: Initial subsidy is LMSR-specific knowledge, so `MarketSystemFactory` should obtain it from `LmsrTradingMechanism` instead of calling `LmsrCalculator.initialSubsidy(...)` directly.
- Design decision: Keep `TradingMechanism` unchanged for this subtask and use the concrete `LmsrTradingMechanism` only while constructing an LMSR event.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/loading/MarketSystemFactory.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: `MarketSystemFactory` now creates one `LmsrTradingMechanism` instance per event and uses it both as the event mechanism and as the source for `calculateInitialSubsidy()`.
- Test result: `mvn compile` passed; `mvn test` passed with 0 JUnit tests discovered; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: `00a6d16f3de11f9e20f285f75a2cda1ed8d00b90`

## LMSR Encapsulation - Subtask 2: Audit Direct LMSR Dependencies

- Goal: Audit direct Engine usages of `LmsrCalculator`, `purchaseCost`, `optionValue`, `initialSubsidy`, and `getB`.
- Rationale: Before changing the common trading contract, direct LMSR dependencies must be classified as valid mechanism internals, mechanism-construction needs, or leakage into other classes.
- Design decision: Do not change `TradingMechanism`, do not remove `getB`, and do not change public API during this audit. The remaining `MarketEvent` dependency on `LmsrTradingMechanism` requires the separate contract decision analyzed in Subtask 3.
- Changed files:
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: No code changes. `purchaseCost`, `optionValue`, and `initialSubsidy` calls are contained in `LmsrTradingMechanism`. `MarketSystemFactory` and `GuessMarketEngineImpl` still use `LmsrCalculator` only to create/configure the LMSR mechanism. `MarketEvent.getB()` delegates to the concrete LMSR mechanism and remains a public LMSR-specific leak that is not safe to remove in this subtask.
- Test result: Not run for this documentation-only audit; Subtask 1 checks passed before this audit.
- Commit ID: `ae778788de4e58e52b2a78090762e70bd4ae2f24`

## LMSR Encapsulation - Subtask 3: TradingMechanism Contract Analysis

- Goal: Analyze which operations belong in a common `TradingMechanism` contract shared by LMSR and a future order book mechanism.
- Rationale: The common contract must not force LMSR-only details such as `getB` or initial subsidy onto mechanisms that may not have them.
- Design decision: No contract implementation was selected. A user-approved architectural decision is required before changing `TradingMechanism` or `MarketEvent`.
- Changed files:
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: No code changes. Common candidates are mechanism identity and trade execution at the event boundary. LMSR-specific behavior includes `b`, cost-function pricing, and initial subsidy. Order book-specific behavior may include limit orders, matching, bid/ask state, and may not include initial subsidy.
- Contract option 1: Add common trade execution and option valuation methods to `TradingMechanism`; keep initial subsidy outside the shared contract as LMSR-specific construction behavior. This would let `MarketEvent.purchase(...)` and current value mapping delegate polymorphically, but it may overfit the order book if its public operations require prices, order ids, or matching state beyond the current `purchaseShares` shape.
- Contract option 2: Keep `TradingMechanism` minimal and introduce smaller capability interfaces, such as a purchase execution capability and an option valuation capability, implemented only where meaningful. This avoids forcing LMSR-only or order-book-only concepts into one interface, but `MarketEvent` or factory code must decide which capabilities are required for a given public operation.
- Test result: Not run for this documentation-only analysis; no production code was changed.
- Commit ID: This analysis documentation commit; exact hash recorded in the final run summary after commit creation.

## LMSR Encapsulation - Subtask 4: Family-Specific Operations Contract

- Goal: Remove `MarketEvent`'s dependency on the concrete `LmsrTradingMechanism` class while preserving its existing behavior and public API.
- Rationale: LMSR operations should be exposed through a family-specific contract so the minimal `TradingMechanism` contract does not force LMSR concepts onto a future order book mechanism.
- Design decision: Introduce `LmsrTradingOperations extends TradingMechanism` as one contract for the LMSR mechanism family. It contains `executePurchase`, `calculateOptionValue`, `calculateInitialSubsidy`, and `getB`. The `getB` operation belongs here because `b` is an LMSR-specific configuration parameter, not a general trading-mechanism property.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/trading/lmsr/LmsrTradingOperations.java`
  - `Engine/src/main/java/guessmarket/engine/trading/lmsr/LmsrTradingMechanism.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: `LmsrTradingMechanism` implements the LMSR family contract. `MarketEvent` now obtains LMSR behavior through `requireLmsrOperations()` and no longer imports or references the concrete mechanism class. `MarketEvent.getB()` retains its existing signature and behavior.
- Test result: `mvn compile` passed; `mvn test` passed with 0 JUnit tests discovered; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2: Users, Accounts, Positions, and Market Maker Assignment

- Status: In progress; Subtasks 1 through 9B and Subtasks 10B.1 through 10B.2 are completed.
- Goal: Add multi-user ownership, private user accounts, per-event market positions, and Market Maker assignment before implementing the Order Book mechanism.
- Rationale: User funds and holdings must have explicit ownership before LMSR can support multiple participants and before a future Order Book can transfer money and shares between users.
- Design decision: Use the user-owned-position model. `MarketSystem` owns users and events; each `User` owns one `UserAccount`; each `UserAccount` owns its `MarketPosition` instances keyed by event id. An event does not keep a second copy of user positions.
- Scope boundary: This stage does not change `TradingMechanism` or `LmsrTradingOperations`, does not implement `OrderBookTradingMechanism`, and does not add JavaFX code to Engine. Assignment 2 XML/XSD/JAXB support is a later subtask in this stage and is not part of the current documentation change.

### Current State - Assignment 1

- There is no `User`, `UserAccount`, or `MarketPosition` domain object. The single Assignment 1 participant is implicit in operations that have no user identity.
- `MarketSystem` owns only `MarketEvent` instances, indexed by event id.
- `MarketEvent` is created as active, owns one `EventAccount`, and performs purchases and closure without identifying the acting user.
- `MarketOption.purchasedShares` stores the aggregate number of shares purchased for each option. It is event-level LMSR state, not a personal holding.
- `Trade`, `PurchaseOutcome`, `CloseOutcome`, and their DTOs contain no user identity.
- `EventAccount` receives purchase cost and commission together and settles one aggregate winning payout. No private user balance is debited or credited.
- The Assignment 1 XML, JAXB model, `MarketDefinition`, and `EventDefinition` define events and LMSR configuration only. They contain no users, initial user balances, or Market Maker assignments.
- A valid XML load atomically replaces the current event system, but there is currently no user state to replace.

### Target State - Assignment 2

#### Ownership and Identity

- `MarketSystem` owns both the user registry and the market-event registry.
- `User` owns a trimmed, non-empty `name` and one `UserAccount`.
- The trimmed user name is the user identifier. Uniqueness is case-sensitive; names that differ only by letter case are distinct.
- `UserAccount` owns `balance`, `UserStatus`, and a map of `MarketPosition` instances keyed by event id.
- `MarketPosition` stores holdings and payments for one user in one event. For each option it keeps share quantity and `amountPaid`, where `amountPaid` excludes commission. It also keeps `commissionPaid` separately.
- `MarketOption.purchasedShares` remains the aggregate LMSR quantity. It is not replaced by personal positions, and `MarketEvent` does not own or duplicate those positions.
- A user becomes a participant in an event when the user submits the first action or order, even if a future Order Book order has not executed.

#### Loading and Validation

- Assignment 2 users are created from `GM-users` in the Assignment 2 XML. Each user's balance is initialized from `initial-cash`, which must be greater than zero.
- Market Maker assignments come from `GM-user / GM-market-maker / event id` references.
- Every Market Maker event reference must identify an existing event, and every event must have exactly one Market Maker from the loaded user registry.
- A successful Assignment 2 XML load replaces the entire system, including all users, accounts, positions, events, and assignments. A failed load leaves the previous system unchanged.
- All Assignment 2 input files use the Assignment 2 format. No permanent anonymous or legacy user is required.

#### Market Maker and Event Lifecycle

- Market Maker is a role held by an existing `User` in relation to an event, not a `MarketMaker` subclass.
- The same user may be the Market Maker of multiple events.
- `MarketEvent` stores only the Market Maker user name/identifier. It does not hold a `User` object. `MarketSystem` resolves that identifier against its user registry.
- Only the assigned Market Maker may open or close an event. Engine APIs for opening and closing therefore receive the acting user's identity.
- A blocked Market Maker may not close an event. This follows the requirement that a blocked user cannot perform further actions and creates a known requirements limitation: an event may remain active with no authorized user able to close it. No unrequested administrator or recovery path will be introduced.
- Opening an LMSR event transfers the calculated initial subsidy from the Market Maker's `UserAccount` to the event's `EventAccount`.
- An LMSR event does not receive its subsidy during XML loading. It starts only after its Market Maker successfully opens it.
- Opening an Order Book event will eventually debit the Market Maker for the initial inventory and credit the corresponding shares to the Market Maker's position. That behavior belongs to the later Order Book implementation.
- A Market Maker opening an event must have sufficient funds; otherwise opening is rejected before any state changes.

#### Money and Status Rules

- `UserAccount` represents private user money. `EventAccount` represents only the event's contract funds.
- Trading commissions are transferred to the assigned Market Maker's `UserAccount`; they are not retained as income in `EventAccount`.
- Assignment 2 settlement is rejected atomically if `EventAccount` cannot cover every payout. The account may not become negative, the Market Maker is not charged for a shortfall, and partial payouts are not permitted.
- Assignment 1 and Assignment 2 closing remain separate explicit paths. The existing `MarketEvent.close` and existing close result types remain unchanged for legacy events; multi-user settlement will be orchestrated through `MarketSystem` and will not fall back silently when aggregate shares and user positions disagree.
- Assignment 2 settlement will return a new immutable domain `SettlementOutcome`. The existing domain `CloseOutcome` and DTO `CloseEventResult` remain unchanged for Assignment 1 compatibility.
- Working assumption for ordinary user actions: an action that makes the balance negative completes. Immediately afterward the user is marked `BLOCKED` and cannot perform further actions.
- The insufficient-funds rule for opening an event is stricter: the Market Maker's opening action is rejected in advance and does not create a negative balance.
- Monetary values remain `double` for compatibility with the existing LMSR implementation. Stage 2 will not introduce a cross-cutting `BigDecimal` migration.

### Implementation Plan

Each subtask must compile, pass the relevant tests and Assignment 1 regression checks, update this document, show its diff, and produce one focused commit before the next subtask begins.

1. Completed: Add `UserStatus` and `UserAccount`, including balance transitions, blocking behavior, and focused unit tests.
2. Completed: Add `User` with trimmed case-sensitive identity and ownership of one `UserAccount`.
3. Completed: Add the user registry to `MarketSystem`, including unique-name validation and user lookup, without changing event behavior.
4. Completed: Add `MarketPosition` with per-option holdings and commission-free `amountPaid`; separate commission tracking remains deferred until commission-aware trade integration.
5. Completed: Connect `MarketPosition` ownership to `UserAccount` and expose position bookkeeping and queries through `User` delegation without trading integration.
6. Completed: Link each `MarketEvent` to its Market Maker by user name and resolve that relationship through `MarketSystem`; do not store a `User` reference in the event.
7. Completed for opening: Subtask 7A adds the not-started lifecycle and explicit Assignment 2 creation path; Subtask 7B adds acting-user authorization and LMSR subsidy transfer on open. User-aware close remains a later focused change.
8. Completed: Subtask 8A separates pure LMSR purchase quoting from execution; Subtasks 8B.1 and 8B.2 add transaction primitives, buyer-aware trades, and atomic user-aware purchases; Subtask 8C records purchase commissions in user positions.
9. Completed: Implement multi-user settlement in two focused parts: 9A creates an immutable, non-mutating settlement plan; 9B prevalidates and atomically distributes funds before closing the event.
10. Completed: Subtask 10B.1 adds Task 2 DTO projections and mappers; Subtask 10B.2 exposes them through the public Engine API; Subtask 10B.3 verifies both API generations together and restores Assignment 1 ConsoleUI compatibility.
11. Add Assignment 2 XML/XSD/JAXB mapping, validation, Market Maker assignment, and atomic replacement of users and events.
12. Add multi-user integration tests covering loading, participation, balance changes, blocking, Market Maker authorization, LMSR opening, purchasing, and settlement, while retaining all Assignment 1 regression checks that remain applicable.

- Documentation result: The selected ownership model, Assignment 1 current state, Assignment 2 target state, constraints, and ordered implementation plan are recorded. No production code, XSD, or JAXB files were changed.
- Test result: Not run because this subtask changes documentation only.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 1: User Status and Account

- Status: Completed.
- Goal: Introduce an independent user-account domain model without connecting it to `User`, `MarketSystem`, `MarketEvent`, positions, Market Maker assignment, or trading mechanisms.
- Rationale: Balance ownership and blocked-account behavior need a focused, tested model before users and market positions are introduced.
- API decision: `UserAccount` is final and exposes `credit(double)`, `debit(double)`, `canAfford(double)`, `getBalance()`, and `getStatus()`. There is no public status setter, unblock operation, manual top-up operation, event reference, or position state.
- Validation decision: Initial balance and operation amounts must be positive and finite. Invalid numeric input is rejected with `IllegalArgumentException` before state changes. A debit from an already blocked account is rejected with `EngineException` and `USER_ACCOUNT_BLOCKED`. A non-finite arithmetic result is rejected with `ARITHMETIC_OVERFLOW` before state changes.
- Blocking and credit behavior: A debit that makes an active account negative completes and changes its status to `BLOCKED`. Further debits are rejected. Credits remain permitted for system-driven payouts but never return a blocked account to `ACTIVE`. `canAfford` is read-only and returns false for blocked accounts even when their credited balance covers the requested amount.
- Changed files:
  - `Engine/pom.xml`
  - `Engine/src/main/java/guessmarket/engine/enums/UserStatus.java`
  - `Engine/src/main/java/guessmarket/engine/domain/UserAccount.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserAccountTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added `ACTIVE` and `BLOCKED` statuses, the standalone account model, the focused blocked-account error code, and JUnit 5 test support. No user, event, position, Market Maker, or trading integration was added.
- Test result: Engine compilation passed; Maven ran 15 JUnit 5 tests with 0 failures and 0 errors; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 2: User Domain Entity

- Status: Completed.
- Goal: Add `User` as a standalone domain identity with one owned `UserAccount`, without integrating users into the market system, events, trading, XML, or UI.
- Rationale: The user is the stable identity and owner of private account state. Keeping the account under `User` gives balance and status one clear owner before the registry and trading flows are introduced.
- API decision: `User` is final and exposes `getName()`, `getBalance()`, `getStatus()`, `credit(double)`, `debit(double)`, and `canAfford(double)`. Its constructor accepts a user name and initial balance. The name is required, trimmed once, rejected when blank, and otherwise preserves case for future case-sensitive registry lookup.
- Encapsulation decision: `UserAccount` is held in one private final field and is not returned by the public API. `User` delegates account reads and operations instead of duplicating balance or status fields. Registry identity semantics, equality, Market Maker roles, and positions remain outside this subtask.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/User.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the standalone user entity and focused tests for name validation and trimming, initial account state, delegated account behavior, and absence of duplicated or publicly exposed account state. No integration with other Engine components was added.
- Test result: Engine compilation passed; Maven ran 22 JUnit 5 tests with 0 failures and 0 errors, including 7 `UserTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 3: Market System User Registry

- Status: Completed.
- Goal: Let `MarketSystem` own, register, enumerate, and resolve users by unique name without connecting users to events, trading, XML, or UI.
- Rationale: `MarketSystem` is already the aggregate that owns the event registry and is the planned owner of all loaded system state. Keeping the user registry there provides one authoritative lookup boundary for future Market Maker and trading orchestration.
- Identity decision: Registry keys use the trimmed user name stored by `User`. Lookup names are trimmed and remain case-sensitive, so `Alice` and `alice` are distinct while ` Alice ` resolves to `Alice`.
- Data structure decision: Users are stored in `LinkedHashMap<String, User>` to provide constant-time name lookup and deterministic insertion order for tests and future presentation.
- API decision: `MarketSystem` adds `addUser(User)`, `getUser(String)`, and `getAllUsers()`. Duplicate registration raises `EngineException` with `DUPLICATE_USER_NAME`; missing lookup raises `EngineException` with `USER_NOT_FOUND`.
- Encapsulation decision: The internal map is never exposed. `getAllUsers()` returns an unmodifiable snapshot through `List.copyOf`, and the existing event methods, event-counting `size()`, constructor behavior, and loading flow remain unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketSystemTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the standalone user registry and focused validation while preserving Assignment 1 event ownership and behavior. No default user, positions, Market Maker assignment, trading integration, Engine API, DTO, XML/JAXB, ConsoleUI, or JavaFX change was introduced.
- Test result: Engine compilation passed; Maven ran 31 JUnit 5 tests with 0 failures and 0 errors, including 9 `MarketSystemTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 4: Market Position Model

- Status: Completed.
- Goal: Add a standalone domain model for one user's share holdings and purchase amounts in one event, without connecting it to accounts, events, trading, XML, or UI.
- Rationale: A position is scoped to one event because option numbers and holdings have meaning only within that event. `UserAccount` will later own positions keyed by event id so each user's private financial and holding state has one owner and is not duplicated in `MarketEvent`.
- Holdings structure: `MarketPosition` stores a `LinkedHashMap<Integer, OptionHolding>`. The private immutable `OptionHolding` value keeps share quantity and amount paid together, avoiding parallel maps that could become inconsistent. Unknown positive option numbers report zero shares and zero amount.
- Paid amount decision: `paidAmount` records only the share purchase price and excludes commission, as established for Stage 2. Commission is not accepted or stored by this subtask's minimal purchase-recording API; separate commission tracking remains deferred until commission-aware trade integration supplies that value.
- Invariants: Event ids and option numbers are positive; purchase quantities and paid amounts are positive; paid amounts are finite. Share addition uses exact `long` arithmetic, amount addition must remain finite, and all updated values are calculated before replacing a holding so rejected input or overflow leaves the position unchanged.
- API decision: `MarketPosition` is final and exposes `MarketPosition(int)`, `recordPurchase(int, long, double)`, `getEventId()`, `getSharesForOption(int)`, `getAmountPaidForOption(int)`, `getTotalShares()`, `getTotalAmountPaid()`, and `getOptionNumbers()`. Option numbers are returned as an immutable insertion-ordered snapshot; the private holding type and map are not exposed.
- Error decision: Invalid arguments use `IllegalArgumentException`, matching existing domain validation. Existing `ARITHMETIC_OVERFLOW` is reused for `long` or finite-`double` accumulation overflow, so no new error code was required.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/MarketPosition.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketPositionTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the isolated position model and focused tests for independent option holdings, totals, validation, overflow atomicity, zero-value queries, and immutable snapshots. No account ownership, user/event linkage, trading, DTO, XML/JAXB, ConsoleUI, or JavaFX integration was introduced.
- Test result: Engine compilation passed; Maven ran 45 JUnit 5 tests with 0 failures and 0 errors, including 14 `MarketPositionTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 5: User-Owned Market Positions

- Status: Completed.
- Goal: Let each `UserAccount` own one `MarketPosition` per event and expose position bookkeeping through `User` delegation without connecting positions to events, trading, or loading.
- Ownership decision: The ownership chain is `User -> UserAccount -> Map<eventId, MarketPosition>`. `UserAccount` is the single owner of personal positions; neither `User` nor `MarketEvent` duplicates the map or any holding data.
- Data structure decision: Positions are stored in a private `LinkedHashMap<Integer, MarketPosition>` for direct event lookup and deterministic insertion order. The map and mutable position objects are never returned.
- Account API decision: `UserAccount` adds `hasPosition(int)`, `recordExecutedPurchase(int, int, long, double)`, `getSharesForOption(int, int)`, `getAmountPaidForOption(int, int)`, `getTotalShares(int)`, `getTotalAmountPaid(int)`, and `getPositionEventIds()`.
- User API decision: `User` delegates the same position operations to its private account. The name `recordExecutedPurchase` states that this is bookkeeping for an already completed purchase and does not execute a trade.
- Encapsulation decision: Position queries return scalar values, and event ids are returned as an immutable insertion-ordered snapshot. No API returns the positions map, a mutable `MarketPosition`, or the private `UserAccount`.
- Atomicity decision: For the first purchase in an event, a position is created locally, updated, and added to the account map only after validation succeeds. Existing-position updates rely on `MarketPosition.recordPurchase`, which computes all updated values before mutation. A failed registration therefore leaves no empty position or partial holding change.
- Money and status boundary: `paidAmount` remains commission-free share cost. Position bookkeeping does not debit or credit money, transfer commission, execute trading logic, or change account status; those operations remain deferred to transaction orchestration.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/UserAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/User.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserAccountTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added account-owned positions, immutable event-id snapshots, scalar position queries, and user delegation. `MarketPosition` itself was not changed, and no event, market system, trading, Engine API, DTO, XML/JAXB, ConsoleUI, or JavaFX integration was added.
- Test result: Engine compilation passed; Maven ran 57 JUnit 5 tests with 0 failures and 0 errors, including 26 `UserAccountTest` tests and 8 `UserTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 6: Market Maker Assignment

- Status: Completed.
- Goal: Assign one existing system user as the Market Maker of an existing event while keeping the relationship independent of funding, commission, lifecycle authorization, and loading.
- Role decision: Market Maker is a role held by a `User` for a particular event, not a `User` subclass and not a separate account. The same user may therefore be assigned to multiple events without duplicating identity or financial state.
- Reference decision: `MarketEvent` stores only the canonical user name as a private string. It never stores or returns a `User`; `MarketSystem` resolves the requested name through its user registry and delegates the canonical `User.getName()` value to the event. No parallel event-to-Market-Maker map exists.
- Assignment lifecycle: Assignment is one-time. Any second attempt, including reassignment to the same user, raises `MARKET_MAKER_ALREADY_ASSIGNED` and preserves the original name. This avoids both silent replacement and ambiguous idempotent behavior.
- Unassigned-state decision: The existing constructor and Assignment 1 factory remain unchanged, and a newly created legacy event may temporarily have no Market Maker. `hasMarketMaker()` reports that state; `getMarketMakerName()` raises `MARKET_MAKER_NOT_ASSIGNED` when no assignment exists; `isMarketMaker(String)` returns false for a valid name before assignment.
- Assignment 2 loading boundary: The assignment document requires every event in a valid Assignment 2 input file to have exactly one Market Maker, but does not require that relationship to be a constructor parameter. That completeness check remains deferred to the Assignment 2 XML/JAXB atomic-loading subtask.
- API decision: `MarketEvent` adds public `hasMarketMaker()`, `getMarketMakerName()`, and `isMarketMaker(String)`, plus package-private one-time `assignMarketMaker(String)` for domain orchestration. `MarketSystem` adds public `assignMarketMaker(int, String)`, which resolves the event first, resolves the user second, and delegates the canonical name.
- Identity decision: Market Maker comparisons remain case-sensitive and trim external whitespace. `MarketSystem` supplies the normalized name already owned by `User`, so the event stores a canonical registry identity.
- Error decision: Existing `EVENT_NOT_FOUND` and `USER_NOT_FOUND` are reused. New `MARKET_MAKER_ALREADY_ASSIGNED` and `MARKET_MAKER_NOT_ASSIGNED` codes describe assignment lifecycle failures.
- Deferred behavior: This subtask does not fund an event, debit or credit a user, transfer commission, authorize opening or closing, change purchase or settlement flows, update positions, expose Engine APIs or DTOs, or alter XML/JAXB or UI code.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketMakerAssignmentTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added a one-time, name-based Market Maker relationship while preserving existing constructors, factory behavior, and Assignment 1 event operations. The event contains no `User` reference and assignment leaves user balances, status, and positions unchanged.
- Test result: Engine compilation passed; Maven ran 70 JUnit 5 tests with 0 failures and 0 errors, including 13 `MarketMakerAssignmentTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 7A: Not-Started Event Lifecycle

- Status: Completed.
- Goal: Introduce the Assignment 2 not-started lifecycle and an explicit event-creation path without implementing Market Maker funding or authorization.
- Rationale: Assignment 2 events must exist before trading begins, while Assignment 1 events must retain their existing active-and-funded construction behavior.
- Lifecycle decision: `EventStatus` adds `NOT_STARTED` as the single source of truth for an event that has not opened. No separate `opened`, `funded`, or `legacy` flag is stored. Purchases and closure require `ACTIVE`; attempting either operation on a not-started event raises `EVENT_NOT_STARTED` before changing state.
- Creation decision: The existing public `MarketEvent` constructor remains the legacy Assignment 1 path and still creates an `ACTIVE` event whose `EventAccount` contains the supplied initial subsidy. `MarketEvent.createNotStartedEvent(...)` is the explicit Assignment 2 path and creates a `NOT_STARTED` event with a zero-balance `EventAccount`.
- Subsidy decision: The required LMSR subsidy is not duplicated in a field. `getRequiredInitialSubsidy()` delegates to `LmsrTradingOperations.calculateInitialSubsidy()`, which remains its single source of truth. Funding that amount is deferred to Subtask 7B.
- Compatibility decision: `MarketSystemFactory`, the public `GuessMarketEngine` API, XML/JAXB, trading calculations, and the legacy constructor call sites are unchanged. Assignment 1 loading therefore continues to produce active, funded events.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/enums/EventStatus.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketEventLifecycleTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the not-started state, explicit Assignment 2 factory, mechanism-derived required-subsidy accessor, and lifecycle guards. No funding, Market Maker authorization, Engine API, XML/JAXB, trade, position, or UI integration was added.
- Test result: Engine compilation passed; Maven ran 76 JUnit 5 tests with 0 failures and 0 errors, including 6 `MarketEventLifecycleTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 7B: Authorized and Funded Event Opening

- Status: Completed.
- Goal: Let the assigned Market Maker fund and open an Assignment 2 event through a user-aware `MarketSystem` operation without changing the public Engine API or the Assignment 1 flow.
- Opening API: `MarketSystem.openEvent(int eventId, String userName)` resolves the event and user, enforces lifecycle and Market Maker authorization, verifies the account and funding, and coordinates the state changes. `GuessMarketEngine` remains unchanged at this stage.
- Precondition order: The system resolves the event, resolves the user, requires `NOT_STARTED`, requires an assigned Market Maker, verifies the acting user is that Market Maker, requires an `ACTIVE` user account, obtains the mechanism-derived subsidy, checks `canAfford`, and prevalidates the event-account credit before the first mutation.
- Funding decision: `LmsrTradingOperations.calculateInitialSubsidy()` remains the single source of the required amount. A not-started event begins with a zero event-account balance; opening debits that exact amount from the Market Maker and credits it once to `EventAccount`. Legacy events are already `ACTIVE` and funded, so the new opening operation rejects them before any debit or credit.
- Negative-balance exception: Event opening uses a strict affordability precheck. Insufficient funds raise `INSUFFICIENT_FUNDS` without debiting or blocking the Market Maker, unlike the separately documented rule for ordinary user actions that may complete and then block an overdrawn account.
- Atomicity boundary: `EventAccount` validates a positive finite credit and finite resulting balance before mutation. After the user debit, `MarketEvent.openWithFunding` verifies the event is still openable and the amount still matches the mechanism-derived requirement, credits the event account, and then assigns `ACTIVE`; no operation after the credit can fail. If a runtime failure occurs before the event mutation completes, `MarketSystem` compensates the exact user debit and rethrows the original exception.
- Double-funding prevention: `EventStatus` is the only lifecycle source of truth. Reopening an `ACTIVE` event raises `EVENT_ALREADY_STARTED`, while reopening a `CLOSED` event uses `EVENT_ALREADY_CLOSED`; both happen before money changes. No funded or rollback flag was introduced.
- Error decision: Added `EVENT_ALREADY_STARTED`, `INSUFFICIENT_FUNDS`, and `USER_NOT_MARKET_MAKER`. Existing `EVENT_NOT_FOUND`, `USER_NOT_FOUND`, `MARKET_MAKER_NOT_ASSIGNED`, `USER_ACCOUNT_BLOCKED`, `EVENT_ALREADY_CLOSED`, and `ARITHMETIC_OVERFLOW` are reused.
- Compatibility decision: The existing `MarketEvent` constructor and `MarketSystemFactory` continue to create active, funded Assignment 1 events. The legacy purchase and close APIs, `GuessMarketEngine`, XML/JAXB, DTOs, positions, trades, commissions, and UI modules are unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/EventAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketEventOpeningTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added authorized, prevalidated LMSR event funding and opening with focused compensation, while leaving user-aware closing and all later trading, settlement, loading, DTO, and UI work deferred.
- Test result: Engine compilation passed; Maven ran 86 JUnit 5 tests with 0 failures and 0 errors, including 10 `MarketEventOpeningTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 8A: Pure Purchase Quoting

- Status: Completed.
- Goal: Calculate a complete LMSR purchase quote before any state change so the later user-aware purchase transaction can validate all monetary values before coordinating its mutations.
- Audit result: The existing flow entered through `GuessMarketEngine.purchaseShares`, delegated to `MarketEvent.purchase`, and called `LmsrTradingMechanism.executePurchase`. That mechanism calculated the LMSR cost and immediately changed aggregate option shares. `MarketEvent` then calculated commission and total payment, updated `EventAccount`, added a `Trade`, and returned `PurchaseOutcome`, which the Engine mapped to the existing `PurchaseResult`.
- Failure audit: Lifecycle, quantity, and option validation could fail before pricing; LMSR arithmetic and share accumulation could fail around the first mutation; non-finite commission or total values and account accumulation were not prevalidated. The synchronized Engine entry point serializes Assignment 1 purchases, and the new quote is consumed immediately by `MarketEvent.purchase`, so no quote reservation, locking, or versioning mechanism is introduced.
- Quote boundary: `MarketEvent.quotePurchase(int optionNumber, long quantity)` is package-private and returns a package-private immutable `PurchaseQuote`. It validates event state and request values, obtains a pure LMSR cost, calculates commission and total charge, and verifies the event account can accept the values without changing state.
- Responsibility decision: `LmsrTradingMechanism.calculatePurchaseCost(...)` reads aggregate option quantities and calculates only the LMSR share cost. It no longer mutates `MarketOption`. `MarketEvent` combines that cost with `CommissionPolicy` and request identity, then owns the later aggregate-share, event-account, and trade-history mutations. `CommissionPolicy` and `LmsrCalculator` do not cross these boundaries.
- Quote structure: `PurchaseQuote` contains `eventId`, `optionNumber`, `quantity`, `shareCost`, `commission`, and `totalCharge`. Share cost is the commission-free amount intended for the future `MarketPosition`; commission is separate; total charge is their finite sum. The quote rejects non-finite or negative monetary values and arithmetic overflow.
- Execution decision: The legacy `MarketEvent.purchase` first creates a quote, then adds aggregate option shares once, records the purchase in `EventAccount` once, appends one `Trade`, and returns the existing `PurchaseOutcome` values from that quote. Pricing and commission formulas are not duplicated.
- Purity decision: Quoting does not change option shares, event-account balances or commissions, event status, trade history, users, positions, or Market Maker accounts. Consecutive quotes over the same state are equal. Not-started and closed events reject quotes before pricing or mutation.
- Exposure decision: The quote and quoting method remain inside the domain package. They are not added to `GuessMarketEngine`, DTOs, ConsoleUI, JavaFX, or XML because Subtask 8B will consume a quote immediately inside one transaction rather than expose a potentially stale value.
- Compatibility decision: `GuessMarketEngine.purchaseShares`, `PurchaseResult`, `PurchaseOutcome`, `Trade`, mapping, and observable Assignment 1 prices, commissions, and state updates remain unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/trading/lmsr/LmsrTradingOperations.java`
  - `Engine/src/main/java/guessmarket/engine/trading/lmsr/LmsrTradingMechanism.java`
  - `Engine/src/main/java/guessmarket/engine/domain/PurchaseQuote.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/EventAccount.java`
  - `Engine/src/test/java/guessmarket/engine/domain/PurchaseQuoteTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketEventOpeningTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added pure purchase pricing and immutable quote construction, moved aggregate share mutation out of LMSR, refactored legacy execution to consume the quote, and added pre-mutation numeric validation without integrating users or changing the public Engine API.
- Test result: Engine compilation passed; Maven ran 100 JUnit 5 tests with 0 failures and 0 errors, including 14 `PurchaseQuoteTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 8B.1: Purchase Transaction Primitives and Buyer Identity

- Status: Completed.
- Goal: Add non-mutating prevalidation and prevalidated apply primitives for every aggregate that will participate in a user-aware LMSR purchase, and add safely exposed buyer identity to `Trade`, without composing the transaction in `MarketSystem` yet.
- Rationale: The transaction must discover all expected domain failures before its first mutation. Separating package-private validation from package-private apply lets the orchestrator validate balances, holdings, aggregate shares, and event-account capacity before committing already-approved values.
- Account primitives: `UserAccount` and `User` add package-private validation and apply delegation for debit and credit. Debit validation permits a finite overdraft for an active account; applying it sets `BLOCKED` from the resulting balance. Credit remains passive and does not unblock. Existing public `credit` and `debit` retain their behavior by calling both phases together.
- Position primitives: `MarketPosition` separates purchase validation from application. `UserAccount` and `User` provide matching executed-purchase validation and apply delegation. Validation of a first event position uses a temporary position and does not insert an empty position into the account map.
- Event primitives: `MarketOption` separates share-addition overflow validation from application. `EventAccount` adds separate share-cost-only validation and application, leaving the legacy `recordPurchase(shareCost, commission)` path unchanged for Assignment 1.
- Trade identity decision: `Trade` adds immutable `Optional<String> buyerName`. The existing seven-argument constructor remains and produces `Optional.empty()` for legacy Assignment 1 trades. The new constructor requires a non-null, non-blank buyer name, trims it, and stores `Optional.of(canonicalName)` for Assignment 2 trades. No empty or fabricated name is used.
- Trade validation: Trade number, option number, and quantity must be positive; monetary values must be finite and non-negative; total paid must equal share cost plus commission. A user-aware `Trade` can therefore be constructed and validated before transaction mutations begin.
- API boundary: All transaction primitives are package-private and remain inside the domain package. Public account behavior, `GuessMarketEngine`, DTOs, XML/JAXB, and UI code are unchanged. `MarketSystem.purchaseShares` is intentionally deferred to Subtask 8B.2.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/UserAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/User.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketPosition.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketOption.java`
  - `Engine/src/main/java/guessmarket/engine/domain/EventAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/Trade.java`
  - `Engine/src/test/java/guessmarket/engine/domain/PurchaseTransactionPrimitivesTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added pure prevalidation and prevalidated apply operations across the future purchase boundary, plus explicit optional buyer identity with a compatible legacy Trade constructor. No user-aware purchase orchestration or money transfer was added.
- Test result: Engine compilation passed; Maven ran 110 JUnit 5 tests with 0 failures and 0 errors, including 10 `PurchaseTransactionPrimitivesTest` tests; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 8B.2: Atomic User-Aware LMSR Purchases

- Status: Completed.
- Goal: Execute an LMSR purchase for a named system user as one coordinated domain transaction, including user balances, Market Maker commission, personal position bookkeeping, aggregate event shares, event funds, and buyer-aware trade history.
- API decision: `MarketSystem.purchaseShares(String userName, int eventId, int optionNumber, long quantity)` returns the immutable domain `PurchaseOutcome`. Mapping to `PurchaseResult` remains deferred to the Engine/API layer, so the domain system does not depend on DTOs.
- Transaction boundary: `MarketSystem` is the synchronized orchestrator because it owns the user and event registries and can resolve the buyer, event, and assigned Market Maker. `MarketEvent` prepares and applies only its own aggregate changes; account and position mutations remain owned by their respective aggregates.
- Money-flow decision: A buyer other than the Market Maker is debited `shareCost + commission`; `shareCost` is credited to `EventAccount`, commission is credited to the Market Maker's `UserAccount`, and the buyer position records `shareCost` only. When the buyer is also the Market Maker, the commission is a transfer to the same account and is therefore netted out: the account is debited only `shareCost`, while the event account and position receive the same values as any other purchase.
- Account-state decision: An active buyer may complete a finite debit that leaves a negative balance; applying that debit marks the account `BLOCKED`. A blocked buyer cannot initiate another purchase. Passive commission credit to a blocked Market Maker remains permitted and does not unblock the account.
- Prevalidation order: The operation resolves all identities and lifecycle requirements, creates a finite quote, validates the buyer debit, validates a distinct Market Maker commission credit, validates position accumulation, and prepares all event changes before the first mutation. Preparation validates aggregate option-share capacity, share-cost-only event-account capacity, the following trade number, and the buyer-aware `Trade`.
- Commit order: After all expected domain failures have been ruled out, the operation applies the buyer debit, Market Maker credit when distinct, buyer position update, aggregate event shares, event-account share cost, trade append, and next trade number. The apply methods operate only on the values that were already validated.
- Atomicity decision: Quote construction and every validation phase are non-mutating. Overflow tests cover the buyer account, Market Maker credit, position values, aggregate option shares, event account, and trade numbering. Rejected operations preserve both user balances and statuses, event-account balance and commission state, option shares, positions, event lifecycle, and trade history.
- Overflow fixture decision: The controlled test mechanism returns `Double.MAX_VALUE / 20.0`. Its share cost, 10-percent commission, and total charge are finite, while adding the finite commission to a `Double.MAX_VALUE` Market Maker balance or adding the finite share cost to a `Double.MAX_VALUE` event balance overflows. This ensures each test reaches its intended target prevalidation rather than failing during quote calculation.
- Compatibility decision: The existing `MarketEvent.purchase` remains the Assignment 1 path and retains legacy event-account treatment and buyer-less trades. It now prevalidates its aggregate share and account changes before applying them. `GuessMarketEngine`, DTOs, XML/JAXB, ConsoleUI, JavaFX, trading contracts, and LMSR formulas are unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserAwarePurchaseTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the user-aware LMSR purchase transaction, distinct money destinations, buyer position recording, buyer-aware trades, and prevalidated event application while preserving the legacy purchase path.
- Test result: Engine compilation passed as part of the Maven run; Maven ran 124 JUnit 5 tests with 0 failures and 0 errors, including 14 `UserAwarePurchaseTest` tests; the focused Market Maker overflow test passed; `EngineSmokeTest` passed with assertions enabled; `git diff --check` passed.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 8C: Commission Bookkeeping in Market Positions

- Status: Completed.
- Goal: Record every commission attributed to a user separately from share cost in the user's per-event position, and carry purchase commission through the atomic user-aware LMSR purchase flow without starting settlement.
- Requirements audit: Assignment 2 requires user details to display total commission paid for participation in an event. It does not prescribe storage granularity. Purchase commission belongs to the purchased option, while future closing commission belongs to the winning holding, so commission is stored per option and summed for the event-level view.
- Representation decision: Each private `MarketPosition.OptionHolding` now contains `shares`, commission-free `amountPaid`, and `commissionPaid`. `getCommissionPaidForOption` exposes the option total and `getTotalCommissionPaid` sums every option without exposing the holding or map.
- Compatibility decision: Existing three-argument `MarketPosition.recordPurchase` and four-argument `UserAccount` and `User.recordExecutedPurchase` methods remain and delegate to new overloads with commission `0.0`. Existing callers therefore retain their previous bookkeeping semantics.
- Purchase-flow decision: `MarketSystem.purchaseShares` supplies `quote.shareCost()` as `paidAmount` and `quote.commission()` as `commissionPaid` to both position prevalidation and application. When the buyer is also the Market Maker, the private account is debited only the net `shareCost`, but the position records the full gross commission calculated in the quote for reporting.
- Validation and atomicity: Commission must be finite and non-negative. Position validation checks share, paid-amount, and commission accumulation before any field is replaced. First-position validation uses a temporary position, so invalid commission cannot leave an empty position. A focused transaction test seeds a finite maximum commission and confirms that commission overflow is raised by position prevalidation before balances, statuses, event funds, option shares, positions, or trade history change.
- Closing-commission preparation: Package-private validate/apply operations can add commission to an existing option holding without changing shares or `amountPaid`. They reject a missing holding and do not create a purchase or position. Subtasks 9A and 9B will use these primitives only after settlement planning and full prevalidation.
- Settlement decisions confirmed before 8C: A blocked Market Maker cannot close; an underfunded event cannot close or pay partially; legacy and multi-user closing use separate explicit paths; and Assignment 2 will use a new immutable domain `SettlementOutcome` while preserving `CloseOutcome` and `CloseEventResult`.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/MarketPosition.java`
  - `Engine/src/main/java/guessmarket/engine/domain/UserAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/User.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/test/java/guessmarket/engine/domain/MarketPositionTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserAccountTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/UserAwarePurchaseTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added per-option commission state, compatible recording overloads, scalar commission queries and delegation, future closing-commission primitives, and gross commission bookkeeping in the existing atomic purchase transaction. No settlement, payout, event closing, Engine API, XML/JAXB, Order Book, or UI code was added.
- Test result: Engine compilation passed; Maven ran 130 JUnit 5 tests with 0 failures and 0 errors, including 19 `MarketPositionTest` tests and 15 `UserAwarePurchaseTest` tests; the focused commission-overflow transaction test passed; `EngineSmokeTest` passed with assertions enabled.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 9A: Pure Multi-User Settlement Planning

- Status: Completed.
- Goal: Calculate the complete Assignment 2 close settlement as an immutable domain plan without changing event lifecycle, event funds, user accounts, positions, commission bookkeeping, aggregate option shares, or trade history.
- Settlement boundary: `MarketSystem.prepareSettlement(int eventId, String actingUserName, int winningOptionNumber)` is package-private and synchronized. It resolves the event and actor, validates the active lifecycle and Market Maker authorization, and creates the complete plan. It is not exposed through `GuessMarketEngine` or DTOs.
- Permission decision: The event must be active and have an assigned Market Maker; only that user may prepare settlement, and a blocked Market Maker is rejected. A blocked winner is still included because payout is passive and planning does not change the winner's `BLOCKED` status. The known assignment limitation remains that an event can stay open if its Market Maker becomes blocked; no administrator or recovery mechanism is invented.
- Payout abstraction: Added the semantic `WinningPayoutOperations` capability without expanding the minimal `TradingMechanism` contract. `LmsrTradingOperations` extends the capability and supplies the LMSR payout of `1.0` per winning share. A future Order Book mechanism can expose its configured `d` through the same capability, leaving the settlement algorithm independent of concrete mechanism classes.
- Plan structure: Package-private records `SettlementPlan`, `UserSettlement`, and `AccountCredit` contain immutable scalar values and `List.copyOf` snapshots. The plan records event identity, winning option, Market Maker, pre-close event balance, payout per share, per-winner results, consolidated account credits, gross payout, closing commission, net winner payout, residual, and the Market Maker's commission-plus-residual entitlement.
- Payout formulas: For each winner, `grossPayout = winningShares * payoutPerWinningShare`; `closingCommission` is `CommissionPolicy.calculate(grossPayout)` only for `ON_CLOSE`, otherwise zero; and `netPayout = grossPayout - closingCommission`. Totals reject non-finite values and numeric overflow.
- Position invariant: Before calculating payouts, the plan sums every user's position shares with `Math.addExact` for every event option and compares each total with `MarketOption.purchasedShares`. Any mismatch raises `POSITION_AGGREGATE_MISMATCH`; arithmetic overflow raises `ARITHMETIC_OVERFLOW`. There is no legacy fallback from this Assignment 2 path.
- Event-fund decision: `totalGrossPayout` must not exceed `eventBalanceBefore`; otherwise `INSUFFICIENT_EVENT_FUNDS` rejects the plan. The Market Maker is not charged for a shortfall and partial payout is not planned. `marketMakerResidual = eventBalanceBefore - totalGrossPayout`, and `totalMarketMakerCredit = totalClosingCommission + marketMakerResidual`.
- Conservation decision: The plan verifies `totalWinnerNetPayout + totalClosingCommission + marketMakerResidual = eventBalanceBefore` within a small floating-point tolerance. Consolidated `AccountCredit` values therefore distribute the whole event balance exactly at the domain model's `double` precision.
- Credit consolidation: Winner net payouts are accumulated by canonical user name in deterministic user-registry order. All closing commission and residual are assigned to the Market Maker. When the Market Maker is also a winner, the winner payout and Market Maker entitlement are merged into one credit; zero-value credits and artificial self-transfers are omitted.
- Commission bookkeeping: Each `UserSettlement` carries the closing commission that Subtask 9B will later validate and add to the winner's position under the winning option. Subtask 9A does not invoke the existing commission apply primitive.
- Stale-plan strategy: A prepared plan will not become a caller-held command. Subtask 9B will prepare and apply settlement inside one synchronized `MarketSystem` close operation, preventing external reuse of a plan after state changes.
- Compatibility decision: The existing `MarketEvent.close`, `CloseOutcome`, and `CloseEventResult` remain unchanged for Assignment 1. This subtask does not add `MarketSystem.closeEvent`, `SettlementOutcome`, payouts, account clearing, the `CLOSED` transition, public Engine APIs, XML/JAXB, Order Book implementation, or UI integration.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/trading/WinningPayoutOperations.java`
  - `Engine/src/main/java/guessmarket/engine/trading/lmsr/LmsrTradingOperations.java`
  - `Engine/src/main/java/guessmarket/engine/domain/SettlementPlan.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/SettlementPlanTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added pure, deterministic multi-user settlement planning with authorization, payout semantics, all-option ownership reconciliation, complete funding validation, per-winner results, and consolidated account credits. No settlement mutation or new public API was introduced.
- Test result: Engine compilation passed; Maven ran 153 JUnit 5 tests with 0 failures and 0 errors, including 23 `SettlementPlanTest` tests and all 130 existing tests; `EngineSmokeTest` passed with assertions enabled; the scope and mutation audit and `git diff --check` passed.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 9B: Atomic Multi-User Settlement Execution

- Status: Completed.
- Goal: Execute the Assignment 2 settlement plan as one coordinated domain transaction that records closing commission, credits every recipient, drains the event account, closes the event, and returns an immutable result while preserving the Assignment 1 close path.
- Transaction boundary: `MarketSystem.closeEvent(int eventId, String actingUserName, int winningOptionNumber)` is public and synchronized. It calls the package-private plan builder and completes prepare, prevalidation, and apply while holding the same `MarketSystem` monitor. Callers never supply a `SettlementPlan`, so a previously observed plan cannot be replayed after state changes.
- Prevalidation order: The system prepares a fresh plan, revalidates the active event and unset winning option, confirms the Market Maker and payout-per-share state, reconciles all aggregate option shares and every winner's personal shares with the plan, resolves every credit recipient, validates every passive account credit, validates every positive closing-commission accumulation, verifies the exact event-account balance, and constructs the outcome before the first mutation.
- Exact-credit decision: Non-Market-Maker winner credits retain deterministic registry order. The Market Maker credit is last and uses the remaining representable event balance after those credits, while being checked against the formula-derived Market Maker entitlement within floating-point tolerance. Sequentially summing the final `AccountCredit` list must compare exactly equal to `eventBalanceBefore`; otherwise planning is rejected before mutation.
- Apply order: Positive closing commission is added to each winner's existing holding, consolidated account credits are applied once, `EventAccount` is drained to `0.0`, the winning option is stored, and `EventStatus.CLOSED` is assigned last. All apply methods consume values that were validated and contain no expected domain validation branches.
- Event-account primitive: `validateSettlementDrain(expectedBalance)` requires the exact still-current finite balance and raises `SETTLEMENT_STATE_MISMATCH` if it changed. `applyValidatedSettlementDrain()` assigns zero without changing `totalCommissionCollected`; closing commission belongs to the Market Maker's user account, not to the event account.
- Event-close primitive: `MarketEvent.validateSettlementClose` verifies `ACTIVE`, a valid winning option, and that no winner is already stored. `applyValidatedSettlementClose` stores the winning option and then assigns `CLOSED` as the final logical mutation; it does not calculate payouts, transfer money, or invoke the legacy close path.
- Outcome structure: Public immutable domain record `SettlementOutcome` contains event and winner identity, Market Maker name, immutable `UserSettlement` and consolidated `AccountCredit` snapshots, all payout/commission/residual totals, and event balances before and after settlement. `UserSettlement` and `AccountCredit` are public immutable domain line-item records so the public outcome does not expose inaccessible internal types. `SettlementPlan` remains package-private and is not returned.
- Commission bookkeeping: Each positive `UserSettlement.closingCommission` is prevalidated and added to the winner's existing position under the winning option. It accumulates with purchase commission without changing shares or `amountPaid`, is not written to `EventAccount`, and is applied at most once because subsequent closes fail on `CLOSED` before planning.
- Blocked-user decision: A blocked Market Maker remains unauthorized to close. A blocked winner is included in the plan, receives passive credit through the existing credit primitive, and remains `BLOCKED`.
- Atomicity result: Winner-credit overflow, Market Maker credit overflow, closing-commission overflow, position mismatch, insufficient event funds, invalid authorization, invalid lifecycle, invalid winner, and changed payout state all fail before any apply. Tests compare complete snapshots of balances, statuses, positions, commissions, event funds, event lifecycle, option shares, and trade history after rejected closes.
- Compatibility decision: `MarketEvent.close(int)` and `CloseOutcome` remain the explicit Assignment 1 path with unchanged accounting. Assignment 2 does not infer owners for legacy shares and does not fall back when user positions and aggregate shares differ. `GuessMarketEngine`, `CloseEventResult`, ConsoleUI, XML/JAXB, Order Book, JavaFX, and FXML are unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/domain/AccountCredit.java`
  - `Engine/src/main/java/guessmarket/engine/domain/EventAccount.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketEvent.java`
  - `Engine/src/main/java/guessmarket/engine/domain/MarketSystem.java`
  - `Engine/src/main/java/guessmarket/engine/domain/SettlementOutcome.java`
  - `Engine/src/main/java/guessmarket/engine/domain/SettlementPlan.java`
  - `Engine/src/main/java/guessmarket/engine/domain/UserSettlement.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/domain/SettlementExecutionTest.java`
  - `Engine/src/test/java/guessmarket/engine/domain/SettlementPlanTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added prevalidated atomic multi-user settlement, immutable public domain results, exact full-account distribution, closing-commission bookkeeping, passive blocked-winner payout, complete event-account draining, and a final one-time close transition without changing the legacy close flow.
- Test result: Engine compilation passed; Maven ran 174 JUnit 5 tests with 0 failures and 0 errors, including 21 `SettlementExecutionTest` tests and all 153 existing tests; `EngineSmokeTest` passed with assertions enabled; `git diff --check` and the final atomicity audit passed.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 10B.1: Task 2 DTO Projections and Mappers

- Status: Completed.
- Goal: Add immutable public projections for Task 2 events, users, positions, purchases, and settlements without extending `GuessMarketEngine` or exposing domain objects.
- Rationale: The future Engine API and JavaFX UI need stable snapshots that preserve the Engine boundary. Task 1 DTOs remain unchanged so the legacy API and ConsoleUI can continue to use their existing LMSR-oriented projection.
- Event projection decision: `MarketEventSummary` and `MarketEventDetails` contain mechanism-independent event fields and the explicit `TradingMethod`. `MarketEventDetails` delegates mechanism-specific display data to the sealed `TradingMechanismDetails` contract. `LmsrEventDetails` is the only current implementation and contains `b`, immutable LMSR option snapshots, and immutable newest-first trade snapshots. A later `OrderBookEventDetails` can be added without adding Order Book fields to the common event records.
- Mapping boundary: `MarketEventDtoMapper` selects the mechanism projection through `TradingMethod` and public semantic `MarketEvent` queries. It does not cast to or import `LmsrTradingMechanism`. Mappers that need cross-aggregate lookup receive `MarketSystem` explicitly; none retain mutable global lookup state.
- User projection decision: `UserSummary` exposes only name, balance, and status. `UserDetails` adds immutable Market Maker event ids and per-event `PositionDetails`. A position includes all event options, the user's shares, commission-free `amountPaid`, separate `commissionPaid`, totals, winner information, and only that user's buyer-aware trades. No `UserAccount`, `MarketPosition`, internal map, realized profit/loss, or unexecuted Order Book participation is exposed or invented.
- Event participation decision: `MarketEventDetails.participantPositions` is derived from existing user positions in registry order. It represents participation currently known to the domain. Future Order Book participation caused by an unexecuted first order remains deferred until that domain state exists.
- Transaction-result decision: `UserPurchaseResult` adds canonical buyer identity and post-transaction event and buyer snapshots to the scalar `PurchaseOutcome` values. `SettlementResult`, `UserSettlementResult`, and `AccountCreditResult` copy every public settlement value while preserving the domain's already consolidated Market Maker credit, including when the Market Maker is also a winner.
- Immutability decision: DTOs are records where appropriate. Every collection is copied with `List.copyOf` or `Set.copyOf`, every nested value is another immutable DTO or enum, and mapper output contains no domain or JavaFX type. A mapped snapshot remains unchanged after later domain purchases or settlement mutations.
- Compatibility boundary: `GuessMarketEngine`, `GuessMarketEngineImpl`, all Task 1 DTOs, ConsoleUI, XML/XSD/JAXB, trading contracts, and domain classes are unchanged. `INVALID_USER_NAME` remains deferred to the public API boundary in Subtask 10B.2.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/dto/TradingMechanismDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/LmsrEventDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/MarketEventSummary.java`
  - `Engine/src/main/java/guessmarket/engine/dto/MarketEventDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/UserSummary.java`
  - `Engine/src/main/java/guessmarket/engine/dto/UserDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/PositionDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/OptionPositionDetails.java`
  - `Engine/src/main/java/guessmarket/engine/dto/UserPurchaseResult.java`
  - `Engine/src/main/java/guessmarket/engine/dto/SettlementResult.java`
  - `Engine/src/main/java/guessmarket/engine/dto/UserSettlementResult.java`
  - `Engine/src/main/java/guessmarket/engine/dto/AccountCreditResult.java`
  - `Engine/src/main/java/guessmarket/engine/impl/MarketEventDtoMapper.java`
  - `Engine/src/main/java/guessmarket/engine/impl/UserDtoMapper.java`
  - `Engine/src/main/java/guessmarket/engine/impl/PurchaseDtoMapper.java`
  - `Engine/src/main/java/guessmarket/engine/impl/SettlementDtoMapper.java`
  - `Engine/src/test/java/guessmarket/engine/impl/Task2DtoMapperTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the complete Task 2 projection layer and mapper coverage without changing a public Engine method or any existing Task 1 DTO.
- Test result: Engine compilation passed; Maven ran 182 JUnit 5 tests with 0 failures and 0 errors, including 8 `Task2DtoMapperTest` tests and all 174 existing tests; `EngineSmokeTest` passed with assertions enabled; `git diff --check` and the dependency audit passed.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 10B.2: Task 2 Engine API Operations

- Status: Completed.
- Goal: Expose the existing Task 2 user, event-lifecycle, purchase, and settlement capabilities through `GuessMarketEngine` using the immutable DTO projections introduced in Subtask 10B.1.
- Rationale: UI and other clients must interact through a stable public Engine boundary without receiving domain objects or duplicating transaction rules outside `MarketSystem`.
- API decision: Added `getAllMarketEvents`, `getMarketEventDetails`, `getAllUsers`, `getUserDetails`, user-aware `openEvent`, user-aware `purchaseShares`, and user-aware `closeEvent`. All seven Assignment 1 methods and their DTOs retain their existing signatures and implementation paths. Market Maker assignment is intentionally not exposed and remains a Stage 11 XML-loading responsibility.
- Delegation decision: Each Task 2 operation first requires a loaded system, validates only public-boundary input, invokes exactly one appropriate `MarketSystem` operation, and maps the current result through the 10B.1 mappers. Opening, authorization, account transfers, purchase bookkeeping, settlement planning, atomicity, and mutation ordering remain exclusively in the domain layer.
- User-name boundary: Added `INVALID_USER_NAME`. Task 2 API methods reject null and blank names with `EngineException`, trim leading and trailing whitespace, and preserve case-sensitive identity. System-load validation occurs first, so every operation on an unloaded engine consistently returns `NO_SYSTEM_LOADED` without leaking `NullPointerException` or `IllegalArgumentException`.
- Query decision: Event and user lists preserve `MarketSystem` insertion order and are not sorted by the Engine. Detail operations return fresh immutable snapshots, including LMSR mechanism details, current balances, status, positions, Market Maker event ids, and participant projections.
- Purchase decision: The new overload delegates to `MarketSystem.purchaseShares`, never to the legacy `MarketEvent.purchase`. Its `UserPurchaseResult` includes the canonical buyer name and updated event and buyer snapshots. An active buyer with insufficient balance completes the purchase and becomes `BLOCKED`, matching the established Task 2 rule; the result exposes the negative balance and completed position.
- Settlement decision: The new close overload delegates to atomic `MarketSystem.closeEvent`, never to legacy `MarketEvent.close`. It maps the complete `SettlementOutcome`, including consolidated credits when the Market Maker is also a winner and passive payout to blocked winners without unblocking them.
- Test-construction decision: `GuessMarketEngineImpl` now declares its previously implicit public no-argument constructor explicitly with unchanged production initialization. A package-private constructor accepts a non-null `MarketSystem` for same-package tests, treats it as loaded, and exposes no getter or public injection API.
- Failure behavior: Domain `EngineException` instances and error codes propagate unchanged. The Engine performs no compensation, retry, or post-failure mutation. A controlled failing mechanism verifies that a rejected quote is attempted exactly once and leaves accounts, positions, event funds, aggregate shares, and trades unchanged.
- Compatibility boundary: XML/XSD/JAXB, ConsoleUI, the known `NOT_STARTED` ConsoleUI switch gap, JavaFX, Order Book, domain algorithms, mutation order, Task 1 DTOs, and Market Maker assignment APIs are unchanged.
- Changed files:
  - `Engine/src/main/java/guessmarket/engine/api/GuessMarketEngine.java`
  - `Engine/src/main/java/guessmarket/engine/impl/GuessMarketEngineImpl.java`
  - `Engine/src/main/java/guessmarket/engine/exception/ErrorCode.java`
  - `Engine/src/test/java/guessmarket/engine/impl/Task2EngineApiTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Added the complete public Task 2 Engine facade over the existing domain behavior while preserving explicit legacy paths and preventing domain leakage.
- Test result: Engine compilation passed; Maven ran 200 JUnit 5 tests with 0 failures and 0 errors, including 18 `Task2EngineApiTest` tests and all 182 existing tests; `EngineSmokeTest` passed with assertions enabled; `git diff --check` and the API dependency audit passed.
- Commit ID: Recorded in the final run summary after commit creation.

## Stage 2 - Subtask 10B.3: Engine API and ConsoleUI Compatibility

- Status: Completed. Stage 10 is complete.
- Goal: Verify that the seven Assignment 1 Engine methods and seven Assignment 2 Engine methods coexist without ambiguity or regression, and restore ConsoleUI compilation after `EventStatus.NOT_STARTED` was introduced.
- Rationale: ConsoleUI remains an Assignment 1 client and must continue to compile against the expanded Engine API. Exhaustive status formatting should expose newly added lifecycle states during compilation instead of hiding them behind a default branch.
- Audit result: The only `EventStatus` switch in ConsoleUI was `ConsoleView.formatStatus`. A clean ConsoleUI compilation reproduced one failure because `NOT_STARTED` was missing; no additional compilation failure was found. ConsoleUI imports only `GuessMarketEngine`, Assignment 1 DTOs, enums, and exceptions, and has no direct domain access. Engine has no ConsoleUI or JavaFX dependency.
- Status-formatting decision: `ConsoleView.formatStatus` is now a pure package-private static function so it can be tested directly. Its exhaustive switch explicitly maps `NOT_STARTED` to `Not Started`, `ACTIVE` to `Active`, and `CLOSED` to `Closed`; no default branch was introduced.
- API compatibility decision: Reflection-based tests pin the exact parameter and return types of all fourteen public methods. They verify that the legacy and user-aware purchase and close overloads resolve independently, that every public return type is an approved DTO or collection of DTOs, and that no domain type appears in the interface.
- Assignment 1 regression coverage: A JUnit compatibility flow loads Assignment 1 XML and verifies legacy event summaries, active events, event details, purchase, and close through their unchanged DTOs and signatures. `EngineSmokeTest` independently exercises the same production factory and XML path with assertions enabled.
- Console test infrastructure: ConsoleUI now has a test-scoped JUnit 5 dependency matching Engine. Its three discovered tests cover all `EventStatus` display values. No Task 2 command, menu item, DTO use, or business behavior was added to ConsoleUI, and no ConsoleUI smoke test exists in the repository.
- Combined validation: Engine compilation and local installation passed; Engine ran 204 JUnit 5 tests with 0 failures and 0 errors, including 4 new compatibility tests. ConsoleUI clean compilation passed against the installed Engine and ran 3 JUnit 5 tests with 0 failures and 0 errors. `EngineSmokeTest` passed with assertions enabled. `git diff --check` and both dependency audits passed.
- Compatibility boundary: XML/XSD/JAXB, domain logic, transaction atomicity, Task 1 and Task 2 DTOs, Order Book, JavaFX, FXML, and all Engine implementation paths are unchanged. There is no fallback between legacy and Task 2 operations.
- Changed files:
  - `ConsoleUI/pom.xml`
  - `ConsoleUI/src/main/java/guessmarket/console/ConsoleView.java`
  - `ConsoleUI/src/test/java/guessmarket/console/ConsoleViewStatusTest.java`
  - `Engine/src/test/java/guessmarket/engine/api/EngineApiCompatibilityTest.java`
  - `docs/Assignment_2_Design_Decisions.md`
- Implementation result: Stage 10 now provides two explicit, compatible Engine API generations and a compiling Assignment 1 ConsoleUI with exhaustive lifecycle-status formatting.
- Test result: Engine ran 204 tests and ConsoleUI ran 3 tests, all passing; both modules compiled, the Engine smoke test passed with assertions, and the final scope and dependency audits were clean.
- Commit ID: Recorded in the final run summary after commit creation.
