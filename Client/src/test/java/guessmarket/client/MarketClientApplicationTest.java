package guessmarket.client;

import guessmarket.client.net.ApiException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketClientApplicationTest {
    @Test
    void connectionFailureExplainsHowToReachTheServer() {
        String message = MarketClientApplication.message(new ConnectException());

        assertTrue(message.contains("localhost:8080/Server/api/"));
        assertTrue(message.contains("Start Tomcat"));
    }

    @Test
    void serverErrorRetainsItsMessage() {
        String message = MarketClientApplication.message(
                new ApiException(409, "DUPLICATE_USER_NAME", "Name is already in use."));

        assertEquals("Name is already in use.", message);
    }
}
