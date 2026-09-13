# JavaFX UI Requirements

## 1. Requirement Sources

This specification fixes the planned Assignment 2 UI structure before full FXML and controller implementation.

| Source | Authority and use |
| --- | --- |
| `Guess Market - v3.docx` | Functional authority for Assignment 2 behavior, required information, file loading, user actions, and resize behavior. |
| `ex 2 scetch.pptx` | Official visual reference for the overall screen arrangement. Both slides were rendered to images and inspected visually. The presentation is not stored in the repository. |
| `docs/Assignment_2_Design_Decisions.md` | Authority for implemented ownership, lifecycle, transaction, settlement, API, compatibility, and staging decisions. |
| `GuessMarketEngine` and public DTOs | Authority for capabilities that the JavaFX layer can use now without accessing domain objects. |
| Stage 14A `JavaFXUI` module | The non-modular Maven, resource, FXML, CSS, and launcher foundation that Stage 14B will extend. |

The assignment requires all application input and output to be in English. The sketch colors, hand-drawn borders, and exact proportions are illustrative rather than a required visual theme.

## 2. Screens and Navigation

### 2.1 Persistent application shell

The application uses one resizable window with a persistent header above a two-tab workspace.

The header contains:

- The `Guess Market` title.
- A `Load File` button.
- A read-only display of the currently loaded file path.
- Loading progress and global feedback.

The workspace contains:

- An `Events` tab for a system-wide market overview.
- A `Users` tab for selecting the acting user, inspecting account participation, and initiating user-scoped actions.

The selected tab and local table selections are UI state. They are not persisted in Engine.

### 2.2 Slide 1: Events tab

| Concern | Visual finding and requirement classification |
| --- | --- |
| Main regions | A full-width header and tab strip sit above a two-column Events workspace. This master-detail structure is required. |
| Left region | The left side contains a filter row above an event list. Filtering by mechanism, status, and commission method is required by the assignment. |
| Right region | The right side contains selected-event details. Two option areas are side by side near the top, with participant information below. Event details, mechanism details, participants, and history are required; their exact internal control arrangement is a developer choice. |
| Spatial relationship | The list and details remain visible together. A horizontal `SplitPane` should preserve this relationship while allowing resizing. The sketch's near-even split is illustrative. |
| User actions | Load a file, switch tabs, change filters, select an event, inspect its mechanism data, participants, and trade history. Lifecycle mutations require an acting user and are addressed under the Users tab recommendation. |
| Displayed data | Event identity, status, mechanism, commission, event-account balance, Market Maker, options, LMSR values and shares or future Order Books, participants, and trade history. |
| Placeholders | `Events (can be table or tiles)`, `Event details and trade`, `Option 1 order book`, `Option 2 order book`, and `Participations information` describe content areas, not literal production labels. |
| Binding structure | Header, Events/Users navigation, filters, master-detail layout, two option-specific Order Books, and participant information are binding structural requirements when the relevant feature exists. |
| Free design | Exact colors, fonts, border style, list representation, column widths, icons, spacing, and whether secondary detail sections use subtabs or accordions. |

### 2.3 Slide 2: Users tab

| Concern | Visual finding and requirement classification |
| --- | --- |
| Main regions | The persistent header and tabs remain. The workspace is a narrow user list on the left and a larger user detail area on the right. This master-detail structure is required. |
| User header | The right side identifies the selected user and shows account balance prominently. Name, balance, and status are required. Exact alignment is free. |
| Participation region | The upper detail area shows events in which the user participates or acts as Market Maker. The sketch label is explanatory, not final copy. |
| Event/action region | The lower detail area shows the selected user's position, history, and mechanism-specific actions for one selected event. |
| Spatial relationship | User selection controls the whole right side. Event selection within the user details controls the lower position and action area. |
| User actions | Select a user, select one of that user's events, inspect holdings and payments, purchase LMSR shares, submit future Order Book orders, and perform lifecycle actions when the selected user is that event's Market Maker. |
| Placeholders | `Users Table`, `Single User Details`, `Events Participation / owner`, and `Single event details and trade` are area descriptions rather than required production labels. `owner` means the Market Maker role, not object ownership. |
| Binding structure | User table, account state, event participation/Market Maker membership, and selected-event trade area are required. |
| Free design | Exact colors, proportions, column layouts, empty-state copy, and whether position/history subsections use tables or tabs. |

### 2.4 Functional interpretation

The assignment says participation and event management are performed from the relevant user area after choosing a user and event. Therefore the Events tab is recommended as an observational system overview, while actor-scoped purchase, open, close, and future Order Book actions belong in the Users tab. Duplicating mutations in the Events tab would require an additional acting-user selector that appears in neither the assignment text nor the sketch.

The assignment asks for active participations but also requires closed-event results in user details. The recommended interpretation is to show all known participations, preserve status on each row, and provide an `Active`/`All` view so closed results remain inspectable.

## 3. Proposed Component Hierarchy

The hierarchy uses layout containers and relative sizing. Absolute-coordinate `AnchorPane` layout is not used.

```text
BorderPane mainRoot
+-- top: VBox applicationHeader
|   +-- HBox titleAndLoadBar
|   |   +-- Label appTitleLabel
|   |   +-- Button loadFileButton
|   |   +-- TextField loadedFilePathField
|   +-- ProgressBar loadProgressBar
|   +-- Label globalFeedbackLabel
+-- center: TabPane workspaceTabs
    +-- Tab eventsTab
    |   +-- SplitPane eventsSplitPane
    |       +-- VBox eventBrowserPane
    |       |   +-- GridPane eventFilterBar
    |       |   +-- TableView eventTable
    |       +-- StackPane eventDetailStateStack
    |           +-- VBox eventEmptyState
    |           +-- ScrollPane eventDetailsScrollPane
    |               +-- VBox eventDetailsPane
    |                   +-- GridPane eventHeaderGrid
    |                   +-- StackPane mechanismDetailsStack
    |                   |   +-- VBox lmsrEventPane
    |                   |   +-- VBox orderBookEventPane (future)
    |                   +-- TabPane eventSecondaryTabs
    |                       +-- Tab participantsTab
    |                       +-- Tab tradeHistoryTab
    +-- Tab usersTab
        +-- SplitPane usersSplitPane
            +-- VBox userBrowserPane
            |   +-- TableView userTable
            +-- StackPane userDetailStateStack
                +-- VBox userEmptyState
                +-- ScrollPane userDetailsScrollPane
                    +-- VBox userDetailsPane
                        +-- GridPane userHeaderGrid
                        +-- TableView userEventsTable
                        +-- StackPane userEventStateStack
                            +-- VBox userEventEmptyState
                            +-- VBox userPositionPane
                                +-- TableView userOptionPositionsTable
                                +-- TableView userTradeHistoryTable
                                +-- StackPane userActionStack
                                    +-- VBox lmsrActionPane
                                    +-- VBox orderBookActionPane (future)
                                    +-- HBox marketMakerActionPane
```

Mechanism-specific panes are selected from `TradingMethod` and populated from `TradingMechanismDetails`. Controllers must not cast to Engine implementation classes or receive domain objects.

## 4. Proposed `fx:id` Component Map

### 4.1 Application shell

| `fx:id` | JavaFX type | Parent | Data or action | Source | State rule |
| --- | --- | --- | --- | --- | --- |
| `mainRoot` | `BorderPane` | Scene | Main resizable layout | UI state | Always present. |
| `applicationHeader` | `VBox` | `mainRoot.top` | Persistent header | UI state | Always present. |
| `appTitleLabel` | `Label` | `titleAndLoadBar` | `Guess Market` | Static text | Always present. |
| `loadFileButton` | `Button` | `titleAndLoadBar` | Opens XML `FileChooser` | UI action | Disabled during load. |
| `loadedFilePathField` | `TextField` | `titleAndLoadBar` | Last successfully loaded path | Selected `Path` | Read-only; unchanged after failed load. |
| `loadProgressBar` | `ProgressBar` | `applicationHeader` | Load progress | JavaFX `Task.progressProperty` | Visible and managed only while loading. |
| `globalFeedbackLabel` | `Label` | `applicationHeader` | Success/error summary | `LoadResult` or `EngineException` | Hidden when no feedback; severity style changes. |
| `workspaceTabs` | `TabPane` | `mainRoot.center` | Events and Users navigation | UI state | Content disabled until a system is loaded; tabs remain visible. |
| `eventsTab` | `Tab` | `workspaceTabs` | Events overview | Static text | Always present. |
| `usersTab` | `Tab` | `workspaceTabs` | User activity | Static text | Always present. |

### 4.2 Events tab

| `fx:id` | JavaFX type | Parent | Data or action | Source | State rule |
| --- | --- | --- | --- | --- | --- |
| `eventsSplitPane` | `SplitPane` | `eventsTab` | Resizable master-detail layout | UI state | Always present after FXML load. |
| `eventFilterBar` | `GridPane` | `eventBrowserPane` | Three independent filter groups | Summary fields | Disabled while loading. |
| `allMechanismsToggle` | `ToggleButton` | mechanism group | Include every implemented method | UI filter | Selected initially. |
| `lmsrMechanismToggle` | `ToggleButton` | mechanism group | LMSR only | `TradingMethod.LMSR` | Enabled now. |
| `orderBookMechanismToggle` | `ToggleButton` | mechanism group | Order Book only | Future `TradingMethod` value | Omitted or disabled until Order Book exists. |
| `allStatusesToggle` | `ToggleButton` | status group | All statuses | UI filter | Selected initially. |
| `notStartedStatusToggle` | `ToggleButton` | status group | Not started | `EventStatus.NOT_STARTED` | Available after v2 loading. |
| `activeStatusToggle` | `ToggleButton` | status group | Active | `EventStatus.ACTIVE` | Always available. |
| `closedStatusToggle` | `ToggleButton` | status group | Closed | `EventStatus.CLOSED` | Always available. |
| `allCommissionsToggle` | `ToggleButton` | commission group | All methods | UI filter | Selected initially. |
| `purchaseCommissionToggle` | `ToggleButton` | commission group | On purchase | `CommissionType.ON_PURCHASE` | Always available. |
| `closeCommissionToggle` | `ToggleButton` | commission group | On close | `CommissionType.ON_CLOSE` | Always available. |
| `eventTable` | `TableView<MarketEventSummary>` | `eventBrowserPane` | All filtered events; row selection | `getAllMarketEvents()` | Empty placeholder when no rows. |
| `eventNameColumn` | `TableColumn` | `eventTable` | Name | `MarketEventSummary.name` | Always visible. |
| `eventStatusColumn` | `TableColumn` | `eventTable` | Lifecycle status | `MarketEventSummary.status` | Always visible. |
| `eventMethodColumn` | `TableColumn` | `eventTable` | LMSR or Order Book | `MarketEventSummary.tradingMethod` | Always visible. |
| `eventCommissionColumn` | `TableColumn` | `eventTable` | Type and percentage | Summary commission fields | May use formatted combined text. |
| `eventBalanceColumn` | `TableColumn` | `eventTable` | Event account balance | `MarketEventSummary.accountBalance` | Always visible. |
| `eventDetailStateStack` | `StackPane` | `eventsSplitPane` | Empty, loading, error, or detail state | UI state | Exactly one child state visible. |
| `eventTitleLabel` | `Label` | `eventHeaderGrid` | Selected event name | `MarketEventDetails.name` | Detail state only. |
| `eventDescriptionLabel` | `Label` | `eventHeaderGrid` | Description and close condition | `MarketEventDetails.description` | Wrap text. |
| `eventStatusLabel` | `Label` | `eventHeaderGrid` | Status | `MarketEventDetails.status` | Styled by status, not color alone. |
| `eventMarketMakerLabel` | `Label` | `eventHeaderGrid` | Market Maker name | `marketMakerName` | Explicit `Not assigned` only for legacy data. |
| `eventAccountBalanceLabel` | `Label` | `eventHeaderGrid` | Event funds | `accountBalance` | Always in details. |
| `eventCommissionLabel` | `Label` | `eventHeaderGrid` | Commission type, rate, and collected total | commission fields | Always in details. |
| `eventWinnerLabel` | `Label` | `eventHeaderGrid` | Winning option | winner optionals | Visible only for closed events. |
| `lmsrEventPane` | `VBox` | `mechanismDetailsStack` | LMSR parameter, options, and values | `LmsrEventDetails` | Visible only for LMSR. |
| `lmsrBLabel` | `Label` | `lmsrEventPane` | Liquidity parameter `b` | `LmsrEventDetails.b` | LMSR only. |
| `lmsrOptionsTable` | `TableView<OptionDetails>` | `lmsrEventPane` | Option number, name, value, aggregate shares | `LmsrEventDetails.options` | LMSR only. |
| `orderBookEventPane` | `VBox` | `mechanismDetailsStack` | Two books and per-option metrics | Future `OrderBookEventDetails` | Hidden until Order Book is implemented. |
| `optionOneBookTable` | `TableView` | `orderBookEventPane` | Option 1 orders | Future order DTO | Order Book only. |
| `optionTwoBookTable` | `TableView` | `orderBookEventPane` | Option 2 orders | Future order DTO | Order Book only. |
| `optionOneMetricsGrid` | `GridPane` | `orderBookEventPane` | BID, ASK, LAST, MID, SPREAD | Future option-market DTO | Order Book only. |
| `optionTwoMetricsGrid` | `GridPane` | `orderBookEventPane` | BID, ASK, LAST, MID, SPREAD | Future option-market DTO | Order Book only. |
| `participantTable` | `TableView<PositionDetails>` | `participantsTab` | User, role, per-option shares/value | `participantPositions` | Pending-order participants require Order Book data. |
| `eventTradeHistoryTable` | `TableView<TradeDetails>` | `tradeHistoryTab` | Newest-first executed trades | mechanism details | LMSR supported; Order Book shape is future work. |

### 4.3 Users tab

| `fx:id` | JavaFX type | Parent | Data or action | Source | State rule |
| --- | --- | --- | --- | --- | --- |
| `usersSplitPane` | `SplitPane` | `usersTab` | Resizable master-detail layout | UI state | Always present. |
| `userTable` | `TableView<UserSummary>` | `userBrowserPane` | User selection | `getAllUsers()` | Empty placeholder when no users. |
| `userNameColumn` | `TableColumn` | `userTable` | Canonical name | `UserSummary.name` | Always visible. |
| `userBalanceColumn` | `TableColumn` | `userTable` | Current balance | `UserSummary.balance` | Always visible. |
| `userStatusColumn` | `TableColumn` | `userTable` | Active or blocked | `UserSummary.status` | Always visible. |
| `userDetailStateStack` | `StackPane` | `usersSplitPane` | Empty, loading, error, or details | UI state | Exactly one child state visible. |
| `selectedUserNameLabel` | `Label` | `userHeaderGrid` | Name | `UserDetails.name` | Detail state only. |
| `selectedUserBalanceLabel` | `Label` | `userHeaderGrid` | Current balance | `UserDetails.balance` | Detail state only. |
| `selectedUserStatusLabel` | `Label` | `userHeaderGrid` | Active or blocked | `UserDetails.status` | Styled by status, not color alone. |
| `participationScopeChoice` | `ComboBox` | `userDetailsPane` | `Active` or `All` participations | UI filter | `Active` default; `All` exposes closed results. |
| `userEventsTable` | `TableView` | `userDetailsPane` | Union of positions and MM events; selection | `positions`, `marketMakerEventIds`, event summaries | Empty placeholder if no involvement. |
| `userEventNameColumn` | `TableColumn` | `userEventsTable` | Event name | `PositionDetails` or summary lookup | Always visible. |
| `userEventStatusColumn` | `TableColumn` | `userEventsTable` | Event status | position/summary status | Always visible. |
| `userEventRoleColumn` | `TableColumn` | `userEventsTable` | Participant, Market Maker, or both | `marketMaker`, MM id set | Always visible. |
| `userEventMethodColumn` | `TableColumn` | `userEventsTable` | Mechanism | position/summary method | Always visible. |
| `userOptionPositionsTable` | `TableView<OptionPositionDetails>` | `userPositionPane` | Shares, share cost, commission, winner | `PositionDetails.options` | Visible when a position exists. |
| `userTotalSharesLabel` | `Label` | `userPositionPane` | Aggregate user shares | `totalShares` | Position state only. |
| `userTotalPaidLabel` | `Label` | `userPositionPane` | Share cost only | `totalAmountPaid` | Kept separate from commission. |
| `userTotalCommissionLabel` | `Label` | `userPositionPane` | Gross commission paid | `totalCommissionPaid` | Kept separate from share cost. |
| `userTradeHistoryTable` | `TableView<TradeDetails>` | `userPositionPane` | User's LMSR trades newest first | `PositionDetails.tradesNewestFirst` | LMSR position only. |
| `lmsrOptionChoice` | `ComboBox<OptionSummary>` | `lmsrActionPane` | Purchase option | selected event options | Active LMSR only. |
| `lmsrQuantityField` | `TextField` | `lmsrActionPane` | Positive integral quantity | validated UI input | Active LMSR and active user only. |
| `purchaseSharesButton` | `Button` | `lmsrActionPane` | Execute LMSR purchase | user-aware purchase API | Disabled unless input and state are valid. |
| `orderSideChoice` | segmented `ToggleButton` group | `orderBookActionPane` | BUY or SELL | Future `OrderSide` | Order Book only. |
| `orderOptionChoice` | `ComboBox<OptionSummary>` | `orderBookActionPane` | Order option | selected event options | Order Book only. |
| `orderQuantityField` | `TextField` | `orderBookActionPane` | Positive integral quantity | future order input | Order Book only. |
| `orderPriceField` | `TextField` | `orderBookActionPane` | Price per share | future order input | Order Book only. |
| `submitOrderButton` | `Button` | `orderBookActionPane` | Submit order | future Order Book API | Order Book, active user, active event only. |
| `openEventButton` | `Button` | `marketMakerActionPane` | Open event as selected user | `openEvent` | MM, active user, and `NOT_STARTED`; Engine validates sufficient funds. |
| `winningOptionChoice` | `ComboBox<OptionSummary>` | `marketMakerActionPane` | Winning option | selected event options | MM and active event only. |
| `closeEventButton` | `Button` | `marketMakerActionPane` | Close event as selected user | user-aware `closeEvent` | MM, active user, `ACTIVE`, winner selected. |
| `actionFeedbackLabel` | `Label` | `userActionStack` | Success or failure message | result DTO or `EngineException` | Visible after an action. |

## 5. Event Screen Data Coverage

| Area | Current DTO support | XML v2 dependency | Order Book dependency | API gap |
| --- | --- | --- | --- | --- |
| Event list and selection | `MarketEventSummary` supports all required common fields. | Production loading must create v2 users, MM assignments, and `NOT_STARTED` events. | `TradingMethod` currently contains only LMSR. | None for LMSR. |
| Mechanism/status/commission filters | All fields exist in summaries. Filtering can be local. | `NOT_STARTED` data needs v2 loading. | Order Book enum/data missing. | No server-side filter API is needed. |
| Common event details | `MarketEventDetails` supplies identity, description, status, method, commission, balance, MM, options, winner. | MM and initial state need v2 loading. | Common projection is already extensible. | None. |
| LMSR values and aggregate shares | `LmsrEventDetails.options` supplies current value and purchased shares; `b` is present. | LMSR v2 construction/loading missing. | None. | None. |
| Two Order Books | No DTO. | Order Book XML mapping is needed. | Full Order Book domain and projections are needed. | Future `OrderBookEventDetails` and order DTOs. |
| BID/ASK/LAST/MID/SPREAD | No DTO. | Method parameters come from v2 XML. | Calculation and book state are missing. | Future per-option market metrics DTO. |
| Participants | `participantPositions` covers users with executed positions and their shares/payments. | Users must be loaded from v2 XML. | Pending-order-only participants and an agreed current-holding-value projection are not represented. | Future participant projection must include pending orders; holding-value semantics require a decision. |
| Trade history | LMSR `TradeDetails` exists and is newest first. | Buyer-aware trades require v2 flow. | Order Book trades need buyer/seller and execution-price projections. | Future Order Book trade DTO; LMSR event-wide buyer identity is not currently exposed. |
| Open/close | User-aware methods exist. | Valid MM assignment and `NOT_STARTED` creation depend on v2 loading. | Order Book opening/settlement is missing. | No LMSR gap; placement in Events tab requires an actor decision. |

## 6. User Screen Data Coverage

| Area | Current DTO support | XML v2 dependency | Order Book dependency | API gap |
| --- | --- | --- | --- | --- |
| User table | `UserSummary` supplies name, balance, status. | Production user creation depends on v2 loading. | None. | None. |
| User details | `UserDetails` supplies account state, MM event ids, positions. | Registry and assignments must be loaded. | Future pending-order involvement missing. | None for implemented LMSR. |
| Participating events | `PositionDetails` supplies event data and position totals. | Loaded users/events required. | First unexecuted order does not yet create representable participation. | Future Order Book participation data. |
| MM events | `marketMakerEventIds` exists; names/status can be joined from event summaries. | MM assignment loading missing. | Order Book MM opening missing. | No extra query is required. |
| Per-option holdings | `OptionPositionDetails` supplies shares, `amountPaid`, `commissionPaid`, winner. | Loaded users required. | Sell-side cost/profit semantics are not implemented. | Realized profit/loss projection is future work. |
| LMSR user history | `PositionDetails.tradesNewestFirst` supplies the selected user's buyer-aware history. | User-aware loaded system required. | None. | None. |
| LMSR purchase | `UserPurchaseResult` and user-aware purchase API exist. | Event must be loaded and opened by its MM. | None. | No quote-preview API; the assignment does not require preview. |
| Add money | Explicitly excluded by the assignment. | None. | None. | Must not be added. |
| Block/unblock controls | Blocking is automatic after a completed overdraft; no manual unblock is required. | None. | None. | Must not be added without a new requirement. |
| BUY/SELL order | No API or DTO. | Order Book event loading needed. | Full Order Book implementation needed. | Proposed future order API. |
| MM open/close | User-aware methods exist for LMSR. | Assignments and v2 lifecycle loading needed. | Order Book lifecycle support missing. | No LMSR gap. |

## 7. Engine API and DTO Mapping

| UI action | Engine call | Input/output DTO | Exists | Missing or limitation | Future stage |
| --- | --- | --- | --- | --- | --- |
| Load XML | `loadSystem(Path)` | `Path` / `LoadResult` | Interface exists | Production loader accepts v1 only today; v2 atomic load is missing. | Stage 11B.3 and later v2 loading work. |
| Show all events | `getAllMarketEvents()` | `List<MarketEventSummary>` | Yes | Order Book method value does not exist yet. | Order Book stage. |
| Show event details | `getMarketEventDetails(int)` | `MarketEventDetails` | Yes | Only `LmsrEventDetails` is permitted today. | Add Order Book projections with the mechanism. |
| Show all users | `getAllUsers()` | `List<UserSummary>` | Yes | Production XML does not create users yet. | Stage 11 v2 loading. |
| Show user details | `getUserDetails(String)` | `UserDetails` | Yes | Pending Order Book orders and realized P/L are absent. | Order Book stage. |
| Open event | `openEvent(int, String)` | `MarketEventDetails` | Yes for LMSR | Production MM assignments need v2 load; Order Book opening is absent. | Stage 11 and Order Book stage. |
| Purchase LMSR | `purchaseShares(int, String, int, long)` | `UserPurchaseResult` | Yes | Requires a v2-loaded/opened system in production. | Stage 11 v2 loading. |
| Close event | `closeEvent(int, String, int)` | `SettlementResult` | Yes for LMSR | Order Book payout mechanism is absent. | Order Book stage. |
| Add user funds | None | None | Not required | The assignment explicitly says accounts cannot be reloaded with funds. | Do not implement. |
| Change user status | None | None | Not required | Blocking is automatic and no activation/unblock operation is specified. | Do not implement without a new requirement. |
| Submit BUY | None | Future order input/result DTOs | No | Order Book domain and API do not exist. | Order Book stage. |
| Submit SELL | None | Future order input/result DTOs | No | Order Book domain and API do not exist. | Order Book stage. |
| Cancel order | None | None | Not required | Cancellation is not specified in the assignment. | Do not implement unless clarified. |
| Refresh Order Book | Reuse `getMarketEventDetails(int)` | Future `OrderBookEventDetails` | Partial | Current sealed mechanism projection supports LMSR only. | Order Book stage. |

### 7.1 Proposed future Order Book boundary

No Order Book signature is approved or implemented by this specification. The following is a design candidate only:

```java
// PROPOSED
OrderSubmissionResult submitOrder(
        int eventId,
        String userName,
        int optionNumber,
        OrderSide side,
        long shareQuantity,
        double pricePerShare);
```

`OrderSide`, `OrderSubmissionResult`, `OrderBookEventDetails`, pending-order DTOs, and trade DTOs must be designed with the actual Order Book domain. A unified command is recommended over duplicated BUY and SELL methods because side-specific rules can still be validated while one public transaction boundary owns matching and minting.

## 8. UI States

| State | Display | Enabled | Disabled or hidden | Feedback |
| --- | --- | --- | --- | --- |
| No file loaded | Header, load control, empty welcome state | Load File | Data tables and all actions | Prompt to load an Assignment 2 XML file. |
| Loading | Existing successful data may remain visible but inactive; progress shown | None except window navigation | Load, selection changes, and mutations | `Loading <file>...`; no success path update yet. |
| Load succeeded | New Events and Users snapshots | Filters, selections, state-valid actions | Progress | Success message with counts; loaded path updates. |
| Load failed | Previous system and previous successful path remain | Load File and previous-system interactions | Progress | Detailed `EngineException` message and code; failed path identified separately. |
| No row selected | Master table and empty detail prompt | Filters and table selection | Detail actions | `Select an event` or `Select a user`. |
| Event `NOT_STARTED` | Event and mechanism details | MM open action in selected-user context | Trading and close | Explain that trading starts after the MM opens the event; Engine reports insufficient funding. |
| Event `ACTIVE` | Live mechanism and participation data | Valid trade actions; MM close | Open | Normal active status. |
| Event `CLOSED` | Winner and final positions/history | Inspection only | Open, trade, close | Closed status and winner. |
| User `ACTIVE` | Account and participation data | State-valid user actions | None solely because of user status | Normal active status. |
| User `BLOCKED` | Account remains visible, including passive credits | Inspection only | Purchase, order, open, close | Explain that completed overdraft blocked future actions. |
| Selected user is MM | MM role displayed | Open or close only when lifecycle permits | Invalid lifecycle action | Explain required funds or winner selection where relevant. |
| Selected user is not MM | Participant role displayed | Participant trading when otherwise valid | Open and close | State that only the named MM can manage the event. |
| LMSR event | Values, aggregate shares, LMSR history, quantity purchase form | LMSR actions by state | Order Book controls | Mechanism label `LMSR`. |
| Order Book event | Two books, metrics, holdings, and order form | Future order actions by state | LMSR controls | Mechanism label `Order Book`. |
| Empty list | Table placeholder | Filters or navigation as applicable | Row-dependent actions | Neutral empty-state text, not an error alert. |
| Action succeeded | Refreshed affected event and user snapshots | State-valid next actions | Stale actions | Inline success feedback; settlement may also use a result dialog. |
| Action failed | Existing snapshots retained unless Engine reports a completed state transition | Retry after correction | Action remains disabled for invalid local input | Inline detailed message; Alert for operation-level failures. |

Controls are disabled from authoritative DTO state and validated local input. Hiding is reserved for mechanism-inapplicable regions; disabled controls remain visible when explaining lifecycle or permission restrictions is useful.

## 9. File Loading Flow

1. The user selects `Load File`.
2. A JavaFX `FileChooser` opens with an XML extension filter. It must accept files from any valid directory, including paths containing spaces.
3. The chosen attempt path is shown in loading feedback. `loadedFilePathField` continues to show the last successful path until success.
4. A JavaFX `Task<LoadResult>` calls `GuessMarketEngine.loadSystem(Path)` off the JavaFX Application Thread.
5. `loadProgressBar` binds to the task. Indeterminate progress is acceptable because Engine exposes no incremental progress.
6. The task includes the assignment-required short artificial delay. A one-second delay is recommended; the exact one- or two-second value remains an implementation choice.
7. On success, update the successful path and reload both event and user summaries. Restore selections only when their exact identifiers still exist.
8. On failure, display the `EngineException` code and message in clear English and retain the previous system and UI data.
9. Never clear the previous UI before Engine confirms successful atomic replacement.
10. After a successful replacement, clear stale event, user, and user-event selections and load fresh details for any preserved valid selection.

The delay must occur inside the background task, never on the JavaFX Application Thread.

## 10. Resize and Layout Rules

- Keep the stage resizable; do not use a fixed-size workaround.
- Use horizontal `SplitPane` controls for both master-detail workspaces with initial divider positions near `0.38` for Events and `0.28` for Users.
- Permit divider movement and save it only as transient UI state.
- Wrap detail columns in `ScrollPane` with `fitToWidth=true`; scrolling is preferred over clipping or reducing text below readable size.
- Let master `TableView` controls grow in both directions and use constrained resize where the visible columns can remain legible.
- Give primary name columns more width than status and numeric columns.
- Use wrapped labels for event descriptions and error messages.
- Avoid fixed pixel coordinates and fixed heights for dynamic tables.
- Allow child panes to shrink with sensible minimum widths; when the window is narrow, details scroll instead of overlapping the master list.
- Order Book tables retain horizontal scrolling if all price and identity columns cannot fit.
- The two option books remain side by side at normal width. A future responsive orientation switch may stack them vertically at narrow widths if visual testing shows side-by-side content becomes unusable.
- Validate at minimum around 1280x800, 900x600, and 720x480, plus manual divider extremes.

## 11. FXML and Controller Alternatives

### Alternative 1: One FXML and one `MainController`

| Criterion | Assessment |
| --- | --- |
| Simplicity | Lowest initial file count and direct access to every control. |
| Separation | Weak. Loading, navigation, filters, event details, users, and actions accumulate in one controller. |
| SceneBuilder | One large document becomes difficult to navigate and edit safely. |
| Shared Engine | Trivial because one controller owns it. |
| Refresh | Easy initially, but event/user refresh concerns become coupled. |
| Testing | Requires constructing or bypassing a large controller for focused tests. |
| Boilerplate | Low at first, high maintenance cost later. |
| Assignment fit | Suitable only for a much smaller UI than the required two master-detail workspaces. |

### Alternative 2: Main FXML with included feature views and controllers

Proposed files:

```text
main-view.fxml              MainController
events-view.fxml            EventsController
users-view.fxml             UsersController
```

Mechanism subviews may be added later only when complexity warrants them, for example `lmsr-event-view.fxml` and `order-book-event-view.fxml`.

| Criterion | Assessment |
| --- | --- |
| Simplicity | Requires a small amount of controller coordination. |
| Separation | Strong ownership of application loading, Events state, and Users state. |
| SceneBuilder | Each feature view can be opened and edited independently. |
| Shared Engine | `GuessMarketApplication` supplies one `GuessMarketEngine` to `MainController`; the main controller initializes included child controllers through interface-typed methods. |
| Refresh | `MainController` coordinates full refresh after load; each child owns its own table and selection refresh. |
| Testing | Controllers can be tested independently with a fake or controlled public Engine interface. |
| Boilerplate | Moderate and explicit. Avoid adding one controller per small panel until needed. |
| Assignment fit | Matches the two official screen sketches and the amount of mechanism-specific state. |

### Recommendation

Use Alternative 2. Keep `MainController` responsible for the shared Engine reference, file-loading task, global feedback, and cross-tab refresh. `EventsController` owns filters, event selection, and event projections. `UsersController` owns user selection, participation selection, and actor-scoped actions. Controllers communicate through small callbacks or a coordinator, never by reaching into each other's controls.

The current Stage 14A static `FXMLLoader.load` call will eventually need to become an `FXMLLoader` instance so the application can retrieve the main controller and supply the shared `GuessMarketEngine`. This is a planned UI-only refactor and does not change Engine.

## 12. Visual Design Principles

- Language: English only, as required by the assignment.
- Palette: neutral light surfaces, dark readable text, one restrained accent, semantic success/warning/error colors, and no reliance on color alone.
- Typography: system-friendly `Segoe UI`, clear 20-24px application/title hierarchy, compact 13-16px workspace text, and no viewport-scaled font sizes.
- Spacing: consistent 8px base spacing with 16-24px section padding.
- Status: text plus semantic style for `Not Started`, `Active`, `Closed`, and `Blocked`.
- Selection: a clear table-row highlight with adequate text contrast.
- Feedback: inline banners for ongoing context; Alerts for load failures, rejected transactions, and close confirmation/results.
- Disabled controls: visibly disabled but still labeled; nearby text explains permission or lifecycle reasons where necessary.
- The sketch's bright green, magenta, beige, gray, and hand-drawn outlines are not binding colors or shapes.

## 13. Engine and Data Gaps

### 13.1 XML v2 dependencies

- `loadSystem(Path)` still follows the Assignment 1 loader path.
- Users, initial cash, MM references, v2 commission spelling, and `NOT_STARTED` event construction are not loaded in production.
- Stage 11 must preserve atomic replacement so failed UI loads retain the previous system.
- Until v2 loading is complete, the normal production JavaFX flow cannot populate the already implemented Task 2 user APIs.

### 13.2 Order Book dependencies

- `TradingMethod` contains only `LMSR`.
- `TradingMechanismDetails` permits only `LmsrEventDetails`.
- There is no order, book, matching, minting, BUY/SELL command, base value `d`, initial inventory, pending participation, BID/ASK/LAST/MID/SPREAD, seller identity, or realized P/L projection.
- The UI must not simulate these values. Order Book panes remain absent or explicitly unavailable until the domain and DTOs exist.

### 13.3 Current projection limitations

- `MarketEventDetails.participantPositions` includes users with positions, not future users whose first unexecuted Order Book instruction makes them participants.
- `TradeDetails` does not expose buyer identity. User-specific LMSR history still works because `UserDtoMapper` filters domain trades before creating the public snapshot. Event-wide user attribution and future Order Book buyer/seller attribution require DTO design.
- The assignment asks the Events view for participant holding value, but no mechanism-independent value definition is established. Multiplying LMSR marginal option value by all shares is not necessarily a liquidation value, and Order Book valuation is not implemented. The UI must not label a derived number as holding value until the meaning is approved and projected by the Engine.
- `UserDetails.marketMakerEventIds` contains ids rather than event summaries; the UI can join these ids against `getAllMarketEvents()` without a new API.
- No public cost quote exists before an LMSR purchase. The assignment requires the purchase behavior and resulting information, not a pre-purchase quote, so this is not currently a blocking UI gap.

## 14. Open Decisions Requiring Approval

1. Approve Alternative 2: `main-view.fxml` with `events-view.fxml` and `users-view.fxml`, each with a focused controller.
2. Approve actor-scoped mutations only in the Users tab. The Events tab remains an overview and does not silently act as the event's Market Maker.
3. Approve three segmented `ToggleButton` groups with an explicit `All` value for event filters, following the assignment hint. A `ComboBox` per filter is the simpler alternative.
4. Approve a `TableView` rather than tiles for the event master list, because the required fields support scanning and comparison.
5. Resolve the participation wording by showing all participation records with an `Active`/`All` scope, rather than hiding closed positions that the assignment also requires users to inspect.
6. Choose the artificial load delay within the required range. One second is recommended.
7. Decide during Order Book design whether pending orders are cancellable. Cancellation is not required by the current assignment and no UI or API should be added now.
8. Decide during Order Book DTO design whether one unified `submitOrder(..., OrderSide, ...)` API is accepted.
9. Decide whether the two Order Books should stack vertically at a narrow responsive breakpoint or always remain side by side with scrolling.
10. Define participant `holding value` for LMSR and Order Book. The UI must not invent a valuation formula from available display fields.

## 15. Stage 14B Acceptance Criteria

Stage 14B UI implementation is complete only when:

- The persistent header, XML-only `FileChooser`, successful-path display, background `Task`, progress, required delay, success flow, and failure-preserves-old-system flow work.
- Events and Users are separate tabs and follow the official master-detail arrangements.
- The Events tab supports all three filters with an explicit `All` selection and displays every lifecycle status.
- Event details display common fields and select mechanism-specific content without concrete Engine implementation casts.
- The Users tab displays account state, participation/MM events, per-option shares, share cost, commission, and LMSR history.
- Actor-scoped open, purchase, and close actions use only the public `GuessMarketEngine` interface and immutable DTOs.
- Blocked users and invalid lifecycle/permission combinations cannot initiate actions and receive clear English feedback.
- Lists have empty states; loading, success, and error states do not overlap content.
- The window remains usable at the documented test sizes without clipping or incoherent overlap.
- FXML files remain SceneBuilder-compatible and contain no direct Engine implementation references.
- Engine remains independent of JavaFX, and the UI does not import Engine domain classes.
- Order Book controls and data are implemented only after their domain, API, and DTO contracts exist; no mock market data is used.
- Relevant JavaFX resource/controller tests and full Engine/ConsoleUI compatibility gates pass before the implementation milestone is committed.
