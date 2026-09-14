# Project workflow

- Work only on the currently active Git branch.
- Preserve all unrelated user changes.
- Never use destructive Git commands.
- Implement one logical subtask at a time.
- Before each subtask, state its goal, rationale, affected files,
  validation steps and completion criteria.
- After each subtask, run the relevant compilation and tests.
- Never create a commit if compilation or regression checks fail.
- Create one focused commit for every completed logical subtask.
- Show the diff before each commit.
- Do not push commits without explicit user approval.
- Stop if a test fails, requirements are ambiguous, or a change requires
  a new architectural decision.
- Do not silently change public APIs.
- Keep the Engine independent of ConsoleUI and JavaFX.
- Keep UI code out of the Engine module.
- Preserve compatibility with Assignment 1 unless Assignment 2 explicitly
  changes the required behavior.
- Record each stage goal, rationale, design decision, implementation result,
  test result and commit ID in docs/Assignment\_2\_Design\_Decisions.md.
- FXML files will be designed with Scene Builder.
- JavaFX controllers may reference FXML controls through @FXML and fx:id,
  but FXML must not call Engine implementation classes directly.
- Controllers must communicate with the Engine through its public API.
- Do not add JavaFX dependencies to the Engine module.
