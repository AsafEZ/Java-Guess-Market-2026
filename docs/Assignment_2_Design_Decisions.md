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

- Status: Planned; implementation has not started.
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
- Opening an LMSR event transfers the calculated initial subsidy from the Market Maker's `UserAccount` to the event's `EventAccount`.
- An LMSR event does not receive its subsidy during XML loading. It starts only after its Market Maker successfully opens it.
- Opening an Order Book event will eventually debit the Market Maker for the initial inventory and credit the corresponding shares to the Market Maker's position. That behavior belongs to the later Order Book implementation.
- A Market Maker opening an event must have sufficient funds; otherwise opening is rejected before any state changes.

#### Money and Status Rules

- `UserAccount` represents private user money. `EventAccount` represents only the event's contract funds.
- Trading commissions are transferred to the assigned Market Maker's `UserAccount`; they are not retained as income in `EventAccount`.
- Working assumption for ordinary user actions: an action that makes the balance negative completes. Immediately afterward the user is marked `BLOCKED` and cannot perform further actions.
- The insufficient-funds rule for opening an event is stricter: the Market Maker's opening action is rejected in advance and does not create a negative balance.
- Monetary values remain `double` for compatibility with the existing LMSR implementation. Stage 2 will not introduce a cross-cutting `BigDecimal` migration.

### Implementation Plan

Each subtask must compile, pass the relevant tests and Assignment 1 regression checks, update this document, show its diff, and produce one focused commit before the next subtask begins.

1. Completed: Add `UserStatus` and `UserAccount`, including balance transitions, blocking behavior, and focused unit tests.
2. Add `User` with trimmed case-sensitive identity and ownership of one `UserAccount`.
3. Add `MarketPosition` with per-option holdings, commission-free `amountPaid`, separate `commissionPaid`, and position-level tests.
4. Add the user registry to `MarketSystem`, including unique-name validation and user lookup, without changing event behavior.
5. Link each `MarketEvent` to its Market Maker by user name and resolve that relationship through `MarketSystem`; do not store a `User` reference in the event.
6. Add event open/close operations with acting-user identity, Market Maker authorization, lifecycle validation, and LMSR subsidy transfer on open.
7. Make LMSR purchase orchestration user-aware, updating the user's account and `MarketPosition` while preserving `MarketOption.purchasedShares` as aggregate state.
8. Add user identity to `Trade` and its mapping while preserving event-level trade history.
9. Implement multi-user settlement on event closure, credit winners, transfer commissions and remaining LMSR funds to the Market Maker, and block further event activity.
10. Add user/account/position DTOs and public Engine API operations, including user-aware trading and Market Maker open/close calls.
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
