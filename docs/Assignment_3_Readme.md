# Guess Market - Assignment 3

## Submitters

Name: [ADD FULL NAME]
ID: [ADD ID NUMBER]
Email: [ADD CONTACT EMAIL]
Partner (if applicable): [ADD NAME, ID AND EMAIL]

Source code: https://github.com/AsafEZ/Java-Guess-Market-2026

## Requirements and startup

- Windows with Java 25 on PATH, and Apache Tomcat 10.1 installed. No Maven or IDE is needed to run the packaged application.
- Copy the single Server.war file to Tomcat's webapps directory. Start Tomcat and wait for automatic deployment at http://localhost:8080/Server/ .
- Run Client/run-client.bat. Launch a separate copy for each user. The client automatically connects to http://localhost:8080/Server/api/ . The WAR name and Tomcat port must remain Server.war and 8080.
- If Tomcat cannot start on this Windows/JDK installation with a loopback-selector error, set JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Public in the Tomcat process environment. This setting is host-specific, not an application requirement.
- All state is in memory; restarting Tomcat clears accounts, events, trades, and history. Upload one of the provided Assignment 3 XML files after login.

## Workflow

- Sign in with a unique user name. An existing logged-in name is rejected; the user may retry with another name. There are no passwords or registration screens.
- Any logged-in user may choose and upload a v3 XML file. A successful upload adds its events to the current system and makes the uploader their market maker. Invalid or duplicate event files produce an error and add nothing. The server parses the request stream and does not save the XML file.
- The Events screen filters by mechanism, state, and commission type. Select an event to inspect prices or order book, executions, trades, and participants. The market maker may open and close it; active LMSR events allow share purchases, and active Order Book events allow buy/sell limit orders.
- The Account screen shows other users' names, balances, and market-maker indicators. The private pane shows your own balance, fund deposit, positions, option holdings, trades, and activity ledger. These views refresh about once per second.
- No chat bonus is claimed. The lecturer clarified that support for events with more than two options is not required despite the original aspiration in the assignment document; the supplied v3 schema and examples are accepted.

## Architecture and decisions

- Server.war contains Engine, Protocol, Gson, XML validation resources, servlet routes, and their runtime dependencies. Tomcat supplies the Servlet API. `EngineContextListener` holds one Engine instance for the Tomcat deployment.
- Engine remains independent of UI code. `Assignment3Engine` extends the Assignment 2 API without changing its original methods. Event uploads are cumulative and atomic. Accounts and activity history are memory-only.
- Protocol is a shared, data-only JAR. Servlet `ViewMapper` converts Engine DTOs to flat JSON views; the client never depends on Engine implementation classes.
- Server servlets handle login/session, XML upload, public event/user lists, detailed event/private account reads, deposits, history, and event actions. User identity for private actions is always taken from the HTTP session.
- `MarketApiClient` uses Java HttpClient, Gson, and a cookie manager. `ClientLauncher` initializes the Windows-specific loopback workaround before the JavaFX app starts. `MarketClientApplication` provides navigation, upload, filtering, and polling. `EventDetailPane` and `AccountDetailPane` render method-specific trading and private account data.
- Event changes and account history are polled every second. Background tasks handle network calls without blocking the JavaFX thread. There is no persistent storage and no direct client-to-client communication.

## Main classes and responsibilities

- Engine / `Assignment3Engine`: extends the existing Assignment 2 engine interface with self-account credit and account-history reads; the older API remains available to the server.
- Engine / `GuessMarketEngineImpl`: owns synchronized in-memory users and events, appends a validated XML upload atomically, and records balance/activity changes caused by deposits and trading. It retains the existing LMSR and Order Book behavior.
- Engine / `Assignment3XmlUnmarshaller`: securely reads the uploaded XML stream through JAXB and validates it against the bundled `GM-EX3-Schema.xsd`; invalid XML becomes a meaningful engine error.
- Engine / `Assignment3EventLoader`: maps the validated v3 XML events to the existing event definitions, applies business validation, and constructs new market events. `GuessMarketEngineImpl` appends those events to earlier uploads.
- Engine / `AccountActivityDetails`: immutable data for one account-history row, including action, event, balance change, commission, resulting balance, and time.
- Server / `EngineContextListener`: creates the single shared Engine at Tomcat application startup, stores it in the servlet context, and removes it when the application stops.
- Server / `HttpApi`: shared servlet helper for finding the Engine and session user, and writing UTF-8 JSON success or error responses.
- Server / `LoginServlet`: registers a unique user name, stores it in the HTTP session, and reports a conflict when the name is already in use.
- Server / `EventUploadServlet`: accepts an authenticated user's raw XML request stream, asks the Engine to append its events, and returns upload counts or validation errors; it does not save the uploaded file.
- Server / `EventsServlet` and `EventDetailsServlet`: expose the shared event list and one selected event's method-specific prices, orders, trades, executions, and participants.
- Server / `UsersServlet`: exposes each user's public name, balance, and market-maker indicator without exposing private positions or history.
- Server / `AccountServlet`, `AccountDepositServlet`, and `AccountHistoryServlet`: read the current user's private positions, credit only that user's balance, and return only that user's activity rows, respectively. They take identity from the session.
- Server / `EventActionServlet`: handles authenticated open, LMSR purchase, Order Book order, and close requests; it validates input and passes the session user to the Engine.
- Server / `ViewMapper`: converts Engine DTOs, including LMSR and Order Book details, into the data-only response objects sent as JSON.
- Protocol / `EventView`, `EventDetailsView`, `UserView`, `UserDetailsView`, `AccountActivityView`, `UploadView`, `ActionResultView`, and `ErrorView`: principal data-only wire records shared by server and client. Related option, order, execution, trade, and position records carry nested display data without UI or Engine implementation types.
- Client / `ClientLauncher`: sets the Windows-specific JDK loopback workaround before starting the JavaFX application.
- Client / `MarketApiClient`: sends HTTP requests to the built-in `localhost:8080/Server/api/` URL, keeps the session cookie, and converts JSON responses into Protocol DTOs.
- Client / `ApiException`: preserves HTTP status, server error code, and message so the UI can show an actionable failure.
- Client / `MarketClientApplication`: owns login, Events/Account navigation, XML file choice, public lists and filters, background network tasks, and one-second server polling.
- Client / `EventDetailPane`: renders an event's method-specific data and enables only actions allowed by its state, mechanism, and the current user's market-maker role.
- Client / `AccountDetailPane`: renders the private balance, deposit form, event positions, per-option holdings, trades, and activity history.

The request path is JavaFX pane -> `MarketApiClient` -> Tomcat servlet -> shared Engine -> `ViewMapper` -> JSON Protocol DTO -> JavaFX pane. Login establishes the session cookie; all later private reads and actions use that session rather than a user name supplied in the request body.

## Troubleshooting

- A 404 or connection failure after login usually means Tomcat has not deployed Server.war yet, the WAR was renamed, or port 8080 is occupied. Check Tomcat logs and http://localhost:8080/Server/api/health .
- Java must be version 25. The client directory must retain its lib folder alongside Client.jar and run-client.bat.
- Upload only Assignment 3 XML files. A duplicate event name or invalid XML is rejected as a whole; details appear in the client feedback area.
- The user list reveals only other users' public name/balance/market-maker status; account positions and history remain private to the logged-in session.
