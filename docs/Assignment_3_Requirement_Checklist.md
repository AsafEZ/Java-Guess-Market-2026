# Assignment 3 requirement checklist

Source: `Guess Market - v3.docx`, Assignment 3 section, paragraphs 480-565. The lecturer's later forum clarification supersedes the early multiple-options aspiration: events with more than two options are not required. This checklist covers Assignment 3, not the separate Assignment 4 web-client bonus.

| Requirement | Implementation | Evidence / status |
| --- | --- | --- |
| Central Tomcat server; clients communicate over HTTP only (492-496) | `Server` WAR hosts one Engine instance; `Client` uses `MarketApiClient` and `Protocol`, with no Engine dependency. | WAR dependency inspection and live Tomcat smoke test passed. |
| Unique name login, retry on conflict, no passwords or sign-up (510) | `LoginServlet` registers a unique name in the Engine and returns HTTP 409 for duplicates; JavaFX login retains the form on error. | Session/login HTTP checks in Stage 3; no password fields. |
| XML v3 upload by any user; cumulative events; uploader becomes market maker (512, 523) | `EventUploadServlet` reads the request stream; `Assignment3Engine` validates and atomically appends events under the session user. | `multiple.xml` packaged-WAR smoke test returned 3 added events; earlier tests covered multiple uploads. |
| Do not save uploaded XML on server (513) | Servlet passes `request.getInputStream()` directly to Engine; no uploaded-file write path. | Stage 5 Tomcat temp-directory check found no uploaded file. |
| Assignment 1-style validation, duplicate event name rejection, all-or-nothing failure message (514-516) | Engine v3 loader validates schema/business rules and duplicate names before committing; HTTP maps invalid XML to 400 and duplicate names to 409. | Engine regression suite and Stage 5 malformed/duplicate live HTTP checks passed. |
| Asynchronous upload with no artificial delay (516) | JavaFX upload uses a background `Task` and visible progress state; no sleep. | Client resource/UI tests and code review; visual confirmation remains manual. |
| All event states and both trading methods visible and actionable as self (517, 539) | Event list/filter/detail pane; session-bound open, LMSR purchase, Order Book order, and close endpoints. Actor name is not accepted from the client request. | Stage 10 live two-session tests and Stage 13 control-visibility test passed. |
| Public users: name, balance, market-maker indicator (517-520) | `GET /api/users` Protocol DTO and JavaFX users table. | Stage 6 two-session HTTP checks passed. |
| Private user details, event participation, option holdings and trades (521) | `GET /api/account` derives identity from session; Account Positions tab drills into options and trades. | Stage 9 private endpoint checks and Stage 14 pane regression passed. |
| Current balance, self-deposit, one history row per affected action including commission (522) | Session-bound deposit and Engine ledger; Account History tab polls `GET /api/account/history`. | Stages 7-8 live HTTP and Engine tests; Stage 14 pane regression passed. |
| Automatic pull, no more than 2 seconds as a normal interval (525) | A background scheduler fetches event, user, private account/history and selected event data with a one-second fixed delay. | Code-level interval verified; actual latency depends on network/server response time. |
| In-memory state only; reset on server restart (526-527) | One Engine instance held by Tomcat context, no database or persistence. | Fresh Tomcat startup showed an empty system; restart-based reset follows from the in-memory design and was not separately exercised in the final smoke test. |
| Resize without losing information (529) | Events/Account split panes switch to vertical orientation below 760 px; detail panes scroll, tables retain horizontal scrolling. | Structural implementation verified; desktop visual inspection at multiple sizes remains pending. |
| Reuse prior work where practical (531, 536, 539-541) | Reuses the Assignment 2 Engine and its trading behavior; new HTTP-facing DTO and JavaFX client screens isolate server/client responsibilities. | Dependency inspection confirms separation. Existing Assignment 2 JavaFX controls were not directly reused; this is a recommendation, not a mandatory API contract. |
| WAR module bundles every runtime dependency (535-536, 557-558) | Server WAR includes Engine, Protocol, Gson and XML/JAXB JARs; Servlet API comes from Tomcat. | Final WAR inspection and packaged-WAR Tomcat deployment passed. |
| Separate new JavaFX client module (536, 559) | `Client` JAR plus Protocol, Gson and JavaFX runtime JARs in `Client/lib`, started by `run-client.bat`; default URL knows `localhost:8080/Server`. | Packaged batch stayed running in smoke test; ZIP has exactly one WAR and all client JARs. |
| ZIP and Word/PDF README, principal classes, decisions, GitHub link (557-563) | `target/Assignment3.zip` contains `Server.war`, `Client/`, and `README.docx`, generated from `docs/Assignment_3_Readme.md`. | ZIP/DOCX structure parsed. Submitter name, ID, email, and optional partner are still placeholders; the GitHub branch has not been pushed. |
| Chat bonus (543-553) | Not implemented or claimed. | Explicitly stated in README. |
| More than two event options (489) | Not implemented as a separate feature. | Lecturer explicitly clarified that multi-option events are not required for this assignment; supported supplied v3 files were tested. |

## Remaining before hand-in

1. Replace the submitter placeholders in `docs/Assignment_3_Readme.md`, then rerun `scripts/package-assignment3.ps1` so the DOCX inside the ZIP is current.
2. Manually inspect the JavaFX window at normal and narrow sizes; automated screen capture was unavailable in this execution environment.
3. Push `feature/trading-mechanism` only after explicit approval, then verify that the README's GitHub branch link is accessible to the grader.
