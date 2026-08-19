package guessmarket.console;

import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.exception.EngineException;
import java.util.List;


public final class ConsolePrinter {
    public void PrintMenu(){
        System.out.println("Guess Market");
        System.out.println("1. Load XML");
        System.out.println("2. Show all events");
        System.out.println("3. Show active events");
        System.out.println("4. Show event details");
        System.out.println("5. Purchase shares");
        System.out.println("6. Close event");
        System.out.println("7. Exit");
        System.out.println("Choose one of the options above by typing the wanted option's number.");
    }

    public void PrintLoadResult(LoadResult result){
        System.out.println("System loaded successfully.");
        System.out.println("Events loaded: " + result.loadedEventCount());
        System.out.println("Initial subsiy: %.2f%n" + result.totalInitialSubsidy());

    }public void printEvents(List<EventSummary> events) {
        for (EventSummary event : events) {
            System.out.println("--------------------");
            System.out.println("ID: " + event.id());
            System.out.println("Name: " + event.name());
            System.out.println("Status: " + event.status());
        }
    }

    public void printPurchaseResult(PurchaseResult result) {
        System.out.println("Purchase completed.");

        System.out.printf("Share cost: %.2f%n", result.shareCost());

        System.out.printf("Commission: %.2f%n", result.commission());

        System.out.printf("Total paid: %.2f%n", result.totalPaid());
    }

    public void printError(EngineException exception) {
        System.out.println("Error [" + exception.getErrorCode() + "]: " + exception.getMessage());
    }

}
