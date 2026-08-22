package guessmarket.console;

import java.util.Arrays;
import java.util.Optional;



public enum MenuCommand {
    LOAD_SYSTEM(1, "Load system details from XML"),
    DISPLAY_EVENTS(2, "Display all events"),
    DISPLAY_EVENT_STATUS(3, "Display event trading status"),
    PURCHASE_SHARES(4, "Participate in an event"),
    CLOSE_EVENT(5, "Close an event"),
    EXIT(6, "Exit");

    private final int number;
    private final String description;

    MenuCommand(int number, String description) {
        this.number = number;
        this.description = description;
    }

    public int number() {
        return number;
    }

    public String description() {
        return description;
    }

    public static Optional<MenuCommand> fromNumber(int number) {
        return Arrays.stream(values())
                .filter(command -> command.number == number)
                .findFirst();
    }
}
