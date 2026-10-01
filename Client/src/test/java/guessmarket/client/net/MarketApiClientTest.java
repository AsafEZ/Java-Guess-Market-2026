package guessmarket.client.net;

import com.sun.net.httpserver.HttpServer;
import guessmarket.protocol.UserView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketApiClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loginCookieIsSentOnLaterRequests() throws Exception {
        server = createServer();
        server.createContext("/api/login", exchange -> {
            assertTrue(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8).contains("Alice"));
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=test123; Path=/api");
            respond(exchange, 200,
                    "{\"name\":\"Alice\",\"balance\":0,\"status\":\"ACTIVE\",\"marketMaker\":false}");
        });
        AtomicReference<String> cookie = new AtomicReference<>();
        server.createContext("/api/users", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            respond(exchange, 200,
                    "[{\"name\":\"Alice\",\"balance\":0,\"status\":\"ACTIVE\",\"marketMaker\":false}]");
        });
        server.start();
        MarketApiClient client = client();

        assertEquals("Alice", client.login("Alice").name());
        assertEquals(List.of(new UserView("Alice", 0, "ACTIVE", false)),
                client.users());
        assertTrue(cookie.get().contains("JSESSIONID=test123"));
    }

    @Test
    void uploadStreamsFileBytesAndPreservesErrorMessage() throws Exception {
        server = createServer();
        byte[] xml = "<Guess-Market/>".getBytes(StandardCharsets.UTF_8);
        AtomicReference<byte[]> received = new AtomicReference<>();
        server.createContext("/api/events/upload", exchange -> {
            received.set(exchange.getRequestBody().readAllBytes());
            assertEquals("application/xml",
                    exchange.getRequestHeaders().getFirst("Content-Type"));
            respond(exchange, 200,
                    "{\"uploadedEventCount\":1,\"totalEventCount\":1,\"eventNames\":[\"Event\"]}");
        });
        server.createContext("/api/event/open", exchange -> respond(exchange, 403,
                "{\"errorCode\":\"USER_NOT_MARKET_MAKER\",\"message\":\"Not your event.\"}"));
        server.start();
        Path path = Files.createTempFile("guess-market-test-", ".xml");
        try {
            Files.write(path, xml);
            MarketApiClient client = client();
            assertEquals(1, client.upload(path).uploadedEventCount());
            assertArrayEquals(xml, received.get());
            ApiException error = assertThrows(ApiException.class, () -> client.open(1));
            assertEquals(403, error.status());
            assertEquals("USER_NOT_MARKET_MAKER", error.errorCode());
            assertEquals("Not your event.", error.getMessage());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private HttpServer createServer() throws IOException {
        return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    }

    private MarketApiClient client() {
        URI base = URI.create("http://localhost:" + server.getAddress().getPort() + "/api/");
        return new MarketApiClient(base);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange,
                                int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }
}
