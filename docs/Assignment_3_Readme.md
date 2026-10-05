Bonus implemented: Chat (Assignment 3, 5 points)

# Guess Market - Assignment 3

## Submitters

Name: [ADD FULL NAME]
ID: [ADD ID NUMBER]
Email: [ADD CONTACT EMAIL]
Partner (if applicable): [ADD NAME, ID AND EMAIL]

Source code: https://github.com/AsafEZ/Java-Guess-Market-2026

## Starting Guess Market

- Windows with Java 25 on PATH, and Apache Tomcat 10.1 installed. No Maven or IDE is needed to run the packaged application.
- Copy the single Server.war file to Tomcat's webapps directory. Start Tomcat and wait for automatic deployment at http://localhost:8080/Server/ .
- Run Client/run-client.bat. Launch a separate copy for each user. The client automatically connects to http://localhost:8080/Server/api/ . The WAR name and Tomcat port must remain Server.war and 8080.
- If Tomcat cannot start on this Windows/JDK installation with a loopback-selector error, set JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Public in the Tomcat process environment. This setting is host-specific, not an application requirement.
- All state is in memory; restarting Tomcat clears accounts, events, trades, and history. Upload one of the provided Assignment 3 XML files after login.

## Workflow

- Enter a new user name on the Sign in screen. If that name is already in use, the client shows an error and stays on the same screen so another name can be entered.
- Closing a client window ends its HTTP session but leaves its user in the running server's memory. That name cannot be used again until Tomcat restarts, so keep the window open while working with that account.
- Any logged-in user may choose and upload a v3 XML file. A successful upload adds its events to the current system and makes the uploader their market maker. Invalid or duplicate event files produce an error and add nothing. The server parses the request stream and does not save the XML file.
- The Events screen filters by mechanism, state, and commission type. Select an event to inspect prices or order book, executions, trades, and participants. The market maker may open and close it; active LMSR events allow share purchases, and active Order Book events allow buy/sell limit orders.
- The Account screen shows other users' names, balances, and market-maker indicators. The private pane shows your own balance, fund deposit, positions, option holdings, trades, and activity ledger. These views refresh about once per second.
- The `Chat` tab shows one conversation shared by logged-in users. Its controls and a two-window check are described below.
- Event screens and trading support two-option events. Events with more than two options are not supported.

## Periodic refresh and selection

- The client polls the shared server every second for events, users, the signed-in account, activity history, and the selected event's details. HTTP calls run off the JavaFX thread.
- Incoming data-only DTOs are compared by value with the displayed data. An unchanged response leaves the relevant table untouched, avoiding repeated row replacement and selection changes when the server has no new information.
- When data does change, each affected table saves its selected row by a stable key, replaces the rows, and selects the matching updated row. A row that no longer exists remains unselected. Account position updates suppress intermediate selection callbacks so their option and trade tables do not clear briefly during the replacement.
- Chat is incremental: the client requests only messages after its last received message ID and appends them. This refresh strategy needs no server-side version counter or Engine changes.

## Using and checking the Chat bonus

1. With the `Server.war` from this ZIP running in Tomcat, launch `Client/run-client.bat` twice and leave both windows open.
2. Sign in with two different, unused names. Each window opens on Events; select the `Chat` tab in each. An empty chat displays `No messages yet`.
3. In the first window, type a message of 1 to 500 characters in `Message`, then press `Send` or Enter. The sender name, local time, and text should appear in both windows, normally within about one second. Repeat from the second window; both messages should appear in both windows in the same order.
4. Blank messages cannot be sent, and a message over 500 characters is rejected with feedback. Sending does not block the rest of the interface. There are no private chats, attachments, message edits, deletions, or administrator controls: this bonus is one shared conversation for logged-in users.
5. The server owns message order and sender identity. Chat remains visible when switching tabs, and a newly connected user can read the current server's earlier messages. Restarting or redeploying Tomcat clears chat and all other in-memory state; open clients must then be restarted and signed in again.

If a message does not appear, confirm that both windows use the `Client` directory from this ZIP and that `http://localhost:8080/Server/api/health` responds. An older `Server.war` will not provide the Chat endpoint. The client shows request errors in the header; check Tomcat logs if the server is unavailable.

## Implementation choices

- One Engine instance serves all users in the Tomcat deployment. The Engine keeps accounts, events, trades, and activity in memory; the server keeps chat history separately in memory. Restarting the deployment clears both. XML uploads append new events only after the whole file passes validation, so a failed upload adds nothing.
- Engine has no UI dependency. We extended its existing API for account operations and placed the data-only Protocol records in a separate JAR shared by server and desktop client. `ViewMapper` turns Engine DTOs into JSON responses; the client does not depend on Engine implementation classes.
- The server identifies a user from the HTTP session cookie for private reads and actions, rather than trusting a name in a request. Closing a client ends that session without deleting the in-memory account.
- The desktop client uses background HTTP calls to avoid blocking JavaFX. Its one-second refresh compares incoming records before changing tables and restores selections by stable keys. Chat uses a message ID cursor, so clients receive and append only new messages. Clients communicate through the server, never directly with one another.

## Main classes and responsibilities

- Engine / `Assignment3Engine`: extends the existing Assignment 2 engine interface with self-account credit and account-history reads; the older API remains available to the server.
- Engine / `GuessMarketEngineImpl`: owns synchronized in-memory users and events, appends a validated XML upload atomically, and records balance/activity changes caused by deposits and trading. It retains the existing LMSR and Order Book behavior.
- Engine / `Assignment3XmlUnmarshaller`: securely reads the uploaded XML stream through JAXB and validates it against the bundled `GM-EX3-Schema.xsd`; invalid XML becomes a meaningful engine error.
- Engine / `Assignment3EventLoader`: maps the validated v3 XML events to the existing event definitions, applies business validation, and constructs new market events. `GuessMarketEngineImpl` appends those events to earlier uploads.
- Engine / `AccountActivityDetails`: immutable data for one account-history row, including action, event, balance change, commission, resulting balance, and time.
- Server / `EngineContextListener`: creates the single shared Engine at Tomcat application startup, stores it in the servlet context, and removes it when the application stops.
- Server / `HttpApi`: shared servlet helper for finding the Engine and session user, and writing UTF-8 JSON success or error responses.
- Server / `LoginServlet`: registers a unique user name, stores it in the HTTP session, and reports a conflict when the name is already in use.
- Server / `LogoutServlet`: ends the current HTTP session when the desktop client closes; it does not remove the user account from the in-memory Engine.
- Server / `ChatRoom` and `ChatServlet`: keep a server-ordered, in-memory global message history, accept text only from an authenticated session user, and return all messages or only those after a requested message ID. Clients never communicate directly with one another.
- Server / `EventUploadServlet`: accepts an authenticated user's raw XML request stream, asks the Engine to append its events, and returns upload counts or validation errors; it does not save the uploaded file.
- Server / `EventsServlet` and `EventDetailsServlet`: expose the shared event list and one selected event's method-specific prices, orders, trades, executions, and participants.
- Server / `UsersServlet`: exposes each user's public name, balance, and market-maker indicator without exposing private positions or history.
- Server / `AccountServlet`, `AccountDepositServlet`, and `AccountHistoryServlet`: read the current user's private positions, credit only that user's balance, and return only that user's activity rows, respectively. They take identity from the session.
- Server / `EventActionServlet`: handles authenticated open, LMSR purchase, Order Book order, and close requests; it validates input and passes the session user to the Engine.
- Server / `ViewMapper`: converts Engine DTOs, including LMSR and Order Book details, into the data-only response objects sent as JSON.
- Protocol / `EventView`, `EventDetailsView`, `UserView`, `UserDetailsView`, `AccountActivityView`, `UploadView`, `ActionResultView`, and `ErrorView`: principal data-only wire records shared by server and client. Related option, order, execution, trade, and position records carry nested display data without UI or Engine implementation types.
- Protocol / `ChatMessageView`: data-only chat message containing server sequence ID, sender, text, and timestamp; shared by server and desktop client.
- Client / `ClientLauncher`: sets the Windows-specific JDK loopback workaround before starting the JavaFX application.
- Client / `MarketApiClient`: sends HTTP requests to the built-in `localhost:8080/Server/api/` URL, keeps the session cookie, and converts JSON responses into Protocol DTOs.
- Client / `ChatPane`: presents the shared chat stream and composer, prevents duplicate rows from overlapping polls, and sends new text through `MarketApiClient` on a background task.
- Client / `ApiException`: preserves HTTP status, server error code, and message so the UI can show an actionable failure.
- Client / `MarketClientApplication`: owns login, Events/Account/Chat navigation, XML file choice, public lists and filters, background network tasks, and one-second server polling.
- Client / `EventDetailPane`: renders an event's method-specific data and enables only actions allowed by its state, mechanism, and the current user's market-maker role.
- Client / `AccountDetailPane`: renders the private balance, deposit form, event positions, per-option holdings, trades, and activity history.

The request path is JavaFX pane -> `MarketApiClient` -> Tomcat servlet -> shared Engine -> `ViewMapper` -> JSON Protocol DTO -> JavaFX pane. Login establishes the session cookie; all later private reads and actions use that session rather than a user name supplied in the request body.

## Troubleshooting

- A 404 or connection failure after login usually means Tomcat has not deployed Server.war yet, the WAR was renamed, or port 8080 is occupied. Check Tomcat logs and http://localhost:8080/Server/api/health .
- The Chat tab needs the Server.war from this ZIP. Replacing an older deployed WAR restarts the in-memory application and clears its users, events, balances, and chat; do this before starting a grading session.
- Java must be version 25. The client directory must retain its lib folder alongside Client.jar and run-client.bat.
- Upload only Assignment 3 XML files. A duplicate event name or invalid XML is rejected as a whole; details appear in the client feedback area.
