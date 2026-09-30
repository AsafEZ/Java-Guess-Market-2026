package guessmarket.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import guessmarket.engine.api.Assignment3Engine;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.exception.EngineException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

public final class AccountDepositServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to credit your account.");
            return;
        }
        Assignment3Engine engine = HttpApi.assignment3Engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        double amount;
        try {
            JsonElement body = JsonParser.parseReader(request.getReader());
            if (!body.isJsonObject() || !body.getAsJsonObject().has("amount")
                    || !body.getAsJsonObject().get("amount").isJsonPrimitive()
                    || !body.getAsJsonObject().get("amount").getAsJsonPrimitive().isNumber()) {
                HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "INVALID_REQUEST", "Expected a JSON object with a numeric amount.");
                return;
            }
            amount = body.getAsJsonObject().get("amount").getAsDouble();
        } catch (JsonParseException | NumberFormatException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", "The request body is not valid JSON.");
            return;
        }
        try {
            UserSummary user = engine.creditAccount(userName, amount);
            boolean marketMaker = engine.isSystemLoaded()
                    && engine.getAllMarketEvents().stream().anyMatch(event ->
                            event.marketMakerName().stream().anyMatch(userName::equals));
            HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                    ViewMapper.user(user, marketMaker ? Set.of(userName) : Set.of()));
        } catch (EngineException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode().name(), exception.getMessage());
        }
    }
}
