package guessmarket.console;

import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.OptionDetails;
import guessmarket.engine.dto.OptionSummary;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.dto.TradeDetails;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.enums.EventStatus;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public final class ConsoleView {

    private static final String SEPARATOR = "----------------------------------------";

    private final PrintStream out;
    private final DecimalFormat decimalFormat;

    public ConsoleView(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        decimalFormat = new DecimalFormat("0.00", symbols);
        decimalFormat.setGroupingUsed(false);
    }

    public void printWelcome() {
        out.println();
        out.println("Welcome to Guess Market");
        out.println(SEPARATOR);
        printMenu();
    }

    public void printMenu() {
        out.println();
        out.println("Main Menu");

        for (MenuCommand command : MenuCommand.values()) {
            out.printf("%d. %s%n", command.number(), command.description());
        }
        out.println("Enter command number: ");

    }

    public void printLoadResult(LoadResult result) {
        out.println();
        out.println("The XML file was loaded successfully.");
        out.printf("Source: %s%n", result.sourcePath());
        out.printf("Loaded events: %d%n", result.loadedEventCount());
        out.printf(
                "Total initial subsidy: %s%n",
                formatNumber(result.totalInitialSubsidy())
        );
    }

    public void printEvents(List<EventSummary> events) {
        out.println();
        out.println("Events");
        out.println(SEPARATOR);

        if (events.isEmpty()) {
            out.println("No events are available.");
            return;
        }

        for (EventSummary event : events) {
            printEventSummary(event);
            out.println(SEPARATOR);
        }
    }

    public void printEventChoices(List<EventSummary> events) {
        out.println();
        out.println("Choose an event:");

        for (int index = 0; index < events.size(); index++) {
            EventSummary event = events.get(index);
            out.printf(
                    "%d. %s (Event ID: %d, Status: %s)%n",
                    index + 1,
                    event.name(),
                    event.eventId(),
                    formatStatus(event.status())
            );
        }
    }

    public void printOptionChoices(List<OptionDetails> options) {
        out.println();
        out.println("Choose an option:");

        for (int index = 0; index < options.size(); index++) {
            OptionDetails option = options.get(index);
            out.printf(
                    "%d. %s (Current value: %s)%n",
                    index + 1,
                    option.name(),
                    formatNumber(option.currentValue())
            );
        }
    }

    public void printEventDetails(EventDetails details) {
        out.println();
        out.println("Event Trading Status");
        out.println(SEPARATOR);
        out.printf("Event ID: %d%n", details.eventId());
        out.printf("Name: %s%n", details.name());
        out.printf("Description: %s%n", details.description());
        out.printf("Status: %s%n", formatStatus(details.status()));
        out.printf("Commission: %d%%%n", details.commissionPercentage());
        out.printf(
                "Commission collection: %s%n",
                formatCommissionType(details.commissionType())
        );

        out.println("Current options:");
        for (OptionDetails option : details.options()) {
            out.printf(
                    "  %d. %s - value: %s, purchased shares: %d%n",
                    option.optionNumber(),
                    option.name(),
                    formatNumber(option.currentValue()),
                    option.purchasedShares()
            );
        }

        out.printf(
                "Event account balance: %s%n",
                formatNumber(details.accountBalance())
        );
        out.printf(
                "Total commission collected: %s%n",
                formatNumber(details.totalCommissionCollected())
        );

        if (details.status() == EventStatus.CLOSED
                && details.winningOptionName() != null) {
            out.printf("Winning option: %s%n", details.winningOptionName());
        }

        printTradeHistory(details.tradesNewestFirst());
        out.println(SEPARATOR);
    }

    public void printPurchaseResult(PurchaseResult result) {
        out.println();
        out.println("Purchase completed successfully.");
        out.printf("Purchased shares: %d%n", result.shareQuantity());
        out.printf("Shares cost: %s%n", formatNumber(result.shareCost()));
        out.printf("Commission: %s%n", formatNumber(result.commission()));
        out.printf("Total paid: %s%n", formatNumber(result.totalPaid()));
        printEventDetails(result.updatedEvent());
    }

    public void printCloseEventResult(CloseEventResult result) {
        out.println();
        out.println("The event was closed successfully.");
        out.printf("Winning option: %s%n", result.winningOptionName());
        out.printf("Gross winning payout: %s%n", formatNumber(result.grossPayout()));
        out.printf("Closing commission: %s%n", formatNumber(result.commission()));
        out.printf("Net payout: %s%n", formatNumber(result.netPayout()));
        printEventDetails(result.closedEvent());
    }

    public void printError(String message) {
        out.println();
        out.println("Error: " + message + "\nTry again.");//May need to delete the "Try again"// "
    }

    public void printInformation(String message) {
        out.println();
        out.println(message);
    }

    public void printGoodbye() {
        out.println();
        out.println("Thank you for using Guess Market. Goodbye.");
    }

    private void printEventSummary(EventSummary event) {
        out.printf("Event ID: %d%n", event.eventId());
        out.printf("Name: %s%n", event.name());
        out.printf("Description: %s%n", event.description());
        out.printf("Commission: %d%%%n", event.commissionPercentage());
        out.printf(
                "Commission collection: %s%n",
                formatCommissionType(event.commissionType())
        );
        out.println("Options:");

        for (OptionSummary option : event.options()) {
            out.printf("  %d. %s%n", option.optionNumber(), option.name());
        }

        out.printf("Status: %s%n", formatStatus(event.status()));
    }

    private void printTradeHistory(List<TradeDetails> trades) {
        out.println("Trade history (newest to oldest):");

        if (trades.isEmpty()) {
            out.println("  No trades have been made.");
            return;
        }

        for (TradeDetails trade : trades) {
            out.printf(
                    "  Trade #%d, option: %s, shares: %d, "
                            + "cost: %s, commission: %s, total: %s%n",
                    trade.tradeNumber(),
                    trade.optionName(),
                    trade.shareQuantity(),
                    formatNumber(trade.shareCost()),
                    formatNumber(trade.commission()),
                    formatNumber(trade.totalPaid())
            );
        }
    }

    private String formatNumber(double value) {
        return decimalFormat.format(value);
    }

    static String formatStatus(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> "Not Started";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }

    private String formatCommissionType(CommissionType type) {
        return switch (type) {
            case ON_PURCHASE -> "On purchase";
            case ON_CLOSE -> "On event closure";
        };
    }
}


