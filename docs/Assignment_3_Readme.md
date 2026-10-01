# Guess Market - Assignment 3

## Submitters

Name: [ADD FULL NAME]
ID: [ADD ID NUMBER]
Email: [ADD CONTACT EMAIL]
Partner (if applicable): [ADD NAME, ID AND EMAIL]

Source code: https://github.com/AsafEZ/Java-Guess-Market-2026
Branch: feature/trading-mechanism (ensure this branch is pushed before submission).

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

- Server.war contains Engine, Protocol, Gson, XML validation resources, servlet routes, and their runtime dependencies. Tomcat supplies the Servlet API. `ApplicationContext` holds one Engine instance for the Tomcat deployment.
- Engine remains independent of UI code. `Assignment3Engine` extends the Assignment 2 API without changing its original methods. Event uploads are cumulative and atomic. Accounts and activity history are memory-only.
- Protocol is a shared, data-only JAR. Servlet `ViewMapper` converts Engine DTOs to flat JSON views; the client never depends on Engine implementation classes.
- Server servlets handle login/session, XML upload, public event/user lists, detailed event/private account reads, deposits, history, and event actions. User identity for private actions is always taken from the HTTP session.
- `MarketApiClient` uses Java HttpClient, Gson, and a cookie manager. `ClientLauncher` initializes the Windows-specific loopback workaround before the JavaFX app starts. `MarketClientApplication` provides navigation, upload, filtering, and polling. `EventDetailPane` and `AccountDetailPane` render method-specific trading and private account data.
- Event changes and account history are polled every second. Background tasks handle network calls without blocking the JavaFX thread. There is no persistent storage and no direct client-to-client communication.

## Troubleshooting

- A 404 or connection failure after login usually means Tomcat has not deployed Server.war yet, the WAR was renamed, or port 8080 is occupied. Check Tomcat logs and http://localhost:8080/Server/api/health .
- Java must be version 25. The client directory must retain its lib folder alongside Client.jar and run-client.bat.
- Upload only Assignment 3 XML files. A duplicate event name or invalid XML is rejected as a whole; details appear in the client feedback area.
- The user list reveals only other users' public name/balance/market-maker status; account positions and history remain private to the logged-in session.
