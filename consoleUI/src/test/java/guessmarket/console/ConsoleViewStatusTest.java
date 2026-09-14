package guessmarket.console;

import guessmarket.engine.enums.EventStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleViewStatusTest {

    @Test
    void formatsNotStartedStatus() {
        assertEquals("Not Started", ConsoleView.formatStatus(EventStatus.NOT_STARTED));
    }

    @Test
    void keepsActiveStatusText() {
        assertEquals("Active", ConsoleView.formatStatus(EventStatus.ACTIVE));
    }

    @Test
    void keepsClosedStatusText() {
        assertEquals("Closed", ConsoleView.formatStatus(EventStatus.CLOSED));
    }
}
