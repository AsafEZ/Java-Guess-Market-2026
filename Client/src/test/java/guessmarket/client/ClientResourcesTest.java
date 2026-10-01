package guessmarket.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientResourcesTest {
    @Test
    void applicationStylesheetIsPackaged() {
        assertNotNull(getClass().getResource("/guessmarket/client/application.css"));
    }
}
