# Assignment 2 Design Decisions

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
