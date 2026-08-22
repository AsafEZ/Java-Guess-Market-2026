package guessmarket.console;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.CloseEventResult;
import guessmarket.engine.dto.EventDetails;
import guessmarket.engine.dto.EventSummary;
import guessmarket.engine.dto.LoadResult;
import guessmarket.engine.dto.OptionDetails;
import guessmarket.engine.dto.PurchaseResult;
import guessmarket.engine.exception.EngineException;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class ConsoleController {

    private final GuessMarketEngine engine;
    private final ConsoleInput input;
    private final ConsoleView view;

    private boolean running;

    public ConsoleController(
            GuessMarketEngine engine,
            ConsoleInput input,
            ConsoleView view) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.input = Objects.requireNonNull(input, "input");
        this.view = Objects.requireNonNull(view, "view");
    }

    public void run() {
        running = true;
        view.printWelcome();

        while (running && input.hasMoreInput()) {

            int commandNumber = input.readInt("");//
            Optional<MenuCommand> command = MenuCommand.fromNumber(commandNumber);

            if (command.isEmpty()) {
                view.printError("Unknown command. Please choose a number from 1 to 6.");
                continue;
            }

            try {
                execute(command.get());
            } catch (EngineException exception) {
                view.printError(exception.getMessage());
            } catch (IllegalStateException exception) {
                view.printError(exception.getMessage());
                running = false;
            }
            view.printMenu();
        }

        view.printGoodbye();
    }

    private void execute(MenuCommand command) {
        switch (command) {
            case LOAD_SYSTEM -> loadSystem();
            case DISPLAY_EVENTS -> displayEvents();
            case DISPLAY_EVENT_STATUS -> displayEventStatus();
            case PURCHASE_SHARES -> purchaseShares();
            case CLOSE_EVENT -> closeEvent();
            case EXIT -> running = false;
        }
    }

    private void loadSystem() {
        Path xmlPath = input.readPath("Enter the XML file path: ");
        LoadResult result = engine.loadSystem(xmlPath);
        view.printLoadResult(result);
    }

    private void displayEvents() {
        view.printEvents(engine.getAllEvents());
    }

    private void displayEventStatus() {
        Optional<EventSummary> selectedEvent = chooseEvent(
                engine.getAllEvents(),
                "No events are available."
        );

        selectedEvent
                .map(EventSummary::eventId)
                .map(engine::getEventDetails)
                .ifPresent(view::printEventDetails);
    }

    private void purchaseShares() {
        Optional<EventSummary> selectedEvent = chooseEvent(
                engine.getActiveEvents(),
                "No active events are available for participation."
        );

        if (selectedEvent.isEmpty()) {
            return;
        }

        int eventId = selectedEvent.get().eventId();
        EventDetails details = engine.getEventDetails(eventId);
        view.printEventDetails(details);

        int optionNumber = chooseOption(details.options());
        long shareQuantity = input.readPositiveLong(
                "Enter the number of shares to purchase: "
        );

        PurchaseResult result = engine.purchaseShares(
                eventId,
                optionNumber,
                shareQuantity
        );

        view.printPurchaseResult(result);
    }

    private void closeEvent() {
        Optional<EventSummary> selectedEvent = chooseEvent(
                engine.getActiveEvents(),
                "No active events are available to close."
        );

        if (selectedEvent.isEmpty()) {
            return;
        }

        int eventId = selectedEvent.get().eventId();
        EventDetails details = engine.getEventDetails(eventId);
        view.printEventDetails(details);

        int winningOptionNumber = chooseOption(details.options());
        CloseEventResult result = engine.closeEvent(eventId, winningOptionNumber);
        view.printCloseEventResult(result);
    }

    private Optional<EventSummary> chooseEvent(
            List<EventSummary> events,
            String emptyMessage) {
        if (events.isEmpty()) {
            view.printInformation(emptyMessage);
            return Optional.empty();
        }

        view.printEventChoices(events);
        int position = input.readIntInRange(
                "Enter event choice: ",
                1,
                events.size()
        );

        return Optional.of(events.get(position - 1));
    }

    private int chooseOption(List<OptionDetails> options) {
        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "The selected event has no available options."
            );
        }

        view.printOptionChoices(options);
        int position = input.readIntInRange(
                "Enter option choice: ",
                1,
                options.size()
        );

        return options.get(position - 1).optionNumber();
    }
}
