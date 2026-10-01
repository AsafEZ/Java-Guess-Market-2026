package guessmarket.client.net;

import com.sun.net.httpserver.HttpServer;
import guessmarket.protocol.UserView;
import guessmarket.protocol.ChatMessageView;
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
    void duplicateNameIsRejectedAndLogoutUsesTheSessionCookie() throws Exception {
        server = createServer();
        AtomicReference<String> logoutCookie = new AtomicReference<>();
        server.createContext("/api/login", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
            if (body.contains("Alice")) {
                respond(exchange, 409,
                        "{\"errorCode\":\"DUPLICATE_USER_NAME\",\"message\":\"User name already exists.\"}");
            } else {
                exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=one; Path=/api");
                respond(exchange, 200,
                        "{\"name\":\"Bob\",\"balance\":0,\"status\":\"ACTIVE\",\"marketMaker\":false}");
            }
        });
        server.createContext("/api/session/logout", exchange -> {
            logoutCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        MarketApiClient client = client();
        ApiException conflict = assertThrows(ApiException.class, () -> client.login("Alice"));
        assertEquals(409, conflict.status());
        assertEquals("DUPLICATE_USER_NAME", conflict.errorCode());
        assertEquals("Bob", client.login("Bob").name());
        client.logout();
        assertTrue(logoutCookie.get().contains("JSESSIONID=one"));
    }

    @Test
    void chatPostsTextAndReadsMessagesAfterTheRequestedCursor() throws Exception {
        server = createServer();
        AtomicReference<String> requestedCursor = new AtomicReference<>();
        AtomicReference<String> postedBody = new AtomicReference<>();
        server.createContext("/api/chat", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                requestedCursor.set(exchange.getRequestURI().getQuery());
                respond(exchange, 200,
                        "[{\"id\":4,\"userName\":\"Alice\",\"text\":\"Hello\",\"sentAtEpochMillis\":1000}]");
            } else {
                postedBody.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 201,
                        "{\"id\":5,\"userName\":\"Bob\",\"text\":\"Hi\",\"sentAtEpochMillis\":2000}");
            }
        });
        server.start();
        MarketApiClient client = client();

        assertEquals(List.of(new ChatMessageView(4, "Alice", "Hello", 1000)),
                client.chatAfter(3));
        assertEquals("after=3", requestedCursor.get());
        assertEquals(new ChatMessageView(5, "Bob", "Hi", 2000), client.postChat("Hi"));
        assertTrue(postedBody.get().contains("\"text\":\"Hi\""));
        assertTrue(!postedBody.get().contains("userName"));
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
