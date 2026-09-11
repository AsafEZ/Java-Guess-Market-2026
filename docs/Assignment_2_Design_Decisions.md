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
- Commit ID: This subtask commit; exact hash recorded in the final run summary after commit creation.
