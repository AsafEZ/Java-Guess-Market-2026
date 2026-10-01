package guessmarket.client.net;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import guessmarket.protocol.AccountActivityView;
import guessmarket.protocol.ActionResultView;
import guessmarket.protocol.ChatMessageView;
import guessmarket.protocol.ErrorView;
import guessmarket.protocol.EventDetailsView;
import guessmarket.protocol.EventView;
import guessmarket.protocol.UploadView;
import guessmarket.protocol.UserDetailsView;
import guessmarket.protocol.UserView;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class MarketApiClient {
    public static final URI DEFAULT_BASE = URI.create("http://localhost:8080/Server/api/");

    private final Gson gson = new Gson();
    private final URI base;
    private final HttpClient http;

    public MarketApiClient() {
        this(DEFAULT_BASE);
    }

    public MarketApiClient(URI base) {
        if (!base.toString().endsWith("/")) {
            throw new IllegalArgumentException("API base URI must end with '/'.");
        }
        this.base = base;
        this.http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public UserView login(String userName) throws IOException, InterruptedException {
        return post("login", Map.of("userName", userName), UserView.class);
    }

    public void logout() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(base.resolve("session/logout"))
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        sendRaw(request);
    }

    public UserView session() throws IOException, InterruptedException {
        return get("login", UserView.class);
    }

    public List<EventView> events() throws IOException, InterruptedException {
        return get("events", new TypeToken<List<EventView>>() {}.getType());
    }

    public List<UserView> users() throws IOException, InterruptedException {
        return get("users", new TypeToken<List<UserView>>() {}.getType());
    }

    public List<ChatMessageView> chatAfter(long lastMessageId)
            throws IOException, InterruptedException {
        return get("chat?after=" + lastMessageId,
                new TypeToken<List<ChatMessageView>>() {}.getType());
    }

    public ChatMessageView postChat(String text) throws IOException, InterruptedException {
        return post("chat", Map.of("text", text), ChatMessageView.class);
    }

    public EventDetailsView event(int id) throws IOException, InterruptedException {
        return get("event?id=" + id, EventDetailsView.class);
    }

    public UserDetailsView account() throws IOException, InterruptedException {
        return get("account", UserDetailsView.class);
    }

    public List<AccountActivityView> history() throws IOException, InterruptedException {
        return get("account/history",
                new TypeToken<List<AccountActivityView>>() {}.getType());
    }

    public UserView deposit(double amount) throws IOException, InterruptedException {
        return post("account/deposit", Map.of("amount", amount), UserView.class);
    }

    public UploadView upload(Path xmlFile) throws IOException, InterruptedException {
        HttpRequest request = request("events/upload")
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofFile(xmlFile))
                .build();
        return send(request, UploadView.class);
    }

    public ActionResultView open(int eventId) throws IOException, InterruptedException {
        return post("event/open", Map.of("eventId", eventId), ActionResultView.class);
    }

    public ActionResultView purchase(int eventId, int optionNumber, long quantity)
            throws IOException, InterruptedException {
        return post("event/purchase", Map.of("eventId", eventId,
                "optionNumber", optionNumber, "quantity", quantity), ActionResultView.class);
    }

    public ActionResultView order(int eventId, int optionNumber, String side,
                                  long quantity, double limitPrice)
            throws IOException, InterruptedException {
        return post("event/order", Map.of("eventId", eventId,
                "optionNumber", optionNumber, "side", side,
                "quantity", quantity, "limitPrice", limitPrice), ActionResultView.class);
    }

    public ActionResultView close(int eventId, int winningOptionNumber)
            throws IOException, InterruptedException {
        return post("event/close", Map.of("eventId", eventId,
                "winningOptionNumber", winningOptionNumber), ActionResultView.class);
    }

    private <T> T get(String path, java.lang.reflect.Type type)
            throws IOException, InterruptedException {
        return send(request(path).GET().build(), type);
    }

    private <T> T post(String path, Object body, java.lang.reflect.Type type)
            throws IOException, InterruptedException {
        return send(jsonPost(path, body), type);
    }

    private HttpRequest jsonPost(String path, Object body) {
        return request(path)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body),
                        StandardCharsets.UTF_8))
                .build();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
    }

    private <T> T send(HttpRequest request, java.lang.reflect.Type type)
            throws IOException, InterruptedException {
        return decode(sendRaw(request), type);
    }

    private HttpResponse<String> sendRaw(HttpRequest request)
            throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            ErrorView error;
            try {
                error = gson.fromJson(response.body(), ErrorView.class);
            } catch (JsonParseException exception) {
                error = null;
            }
            throw new ApiException(response.statusCode(),
                    error != null && error.errorCode() != null
                            ? error.errorCode() : "HTTP_ERROR",
                    error != null && error.message() != null
                            ? error.message() : "Server returned HTTP " + response.statusCode());
        }
        return response;
    }

    private <T> T decode(HttpResponse<String> response, java.lang.reflect.Type type)
            throws IOException {
        try {
            return gson.fromJson(response.body(), type);
        } catch (JsonParseException exception) {
            throw new IOException("The server returned invalid JSON.", exception);
        }
    }
}
