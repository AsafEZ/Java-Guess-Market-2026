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

    @Test
    void duplicateLoginTellsTheUserHowToRetry() {
        String message = MarketClientApplication.loginMessage(
                new ApiException(409, "DUPLICATE_USER_NAME", "Duplicate user name: Alice."));

        assertEquals("That user name is already in use. Try another name.", message);
    }

    @Test
    void initialWindowUsesPhysicalPixelsAndFitsSmallerScreens() {
        assertEquals(1270.0 / 1.5,
                MarketClientApplication.initialSceneSize(1270, 1.5, 1280), 0.01);
        assertEquals(785.0 / 1.5,
                MarketClientApplication.initialSceneSize(785, 1.5, 720), 0.01);
        assertEquals(920,
                MarketClientApplication.initialSceneSize(1270, 1, 960), 0.01);
    }
}
