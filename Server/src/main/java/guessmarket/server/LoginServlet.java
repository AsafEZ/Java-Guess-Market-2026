package guessmarket.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

public final class LoginServlet extends HttpServlet {
    static final String USER_NAME_ATTRIBUTE = "guessmarket.userName";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(USER_NAME_ATTRIBUTE) != null) {
            HttpApi.writeError(response, HttpServletResponse.SC_CONFLICT,
                    "ALREADY_LOGGED_IN", "This session is already logged in.");
            return;
        }

        String userName;
        try {
            JsonElement body = JsonParser.parseReader(request.getReader());
            if (!body.isJsonObject() || !body.getAsJsonObject().has("userName")
                    || !body.getAsJsonObject().get("userName").isJsonPrimitive()
                    || !body.getAsJsonObject().get("userName").getAsJsonPrimitive().isString()) {
                HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "INVALID_REQUEST", "Expected a JSON object with a userName string.");
                return;
            }
            userName = body.getAsJsonObject().get("userName").getAsString();
        } catch (JsonParseException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", "The request body is not valid JSON.");
            return;
        }

        HttpSession session = request.getSession(true);
        try {
            UserSummary user = engine.registerUser(userName);
            session.setAttribute(USER_NAME_ATTRIBUTE, user.name());
            response.setHeader("X-Resume-Token",
                    HttpApi.resumeSessions(getServletContext()).register(user.name(), session));
            HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                    ViewMapper.user(user, Set.of()));
        } catch (EngineException exception) {
            int status = exception.getErrorCode() == ErrorCode.DUPLICATE_USER_NAME
                    ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST;
            HttpApi.writeError(response, status,
                    exception.getErrorCode().name(), exception.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "No user is logged in for this session.");
            return;
        }
        UserSummary user = engine.getAllUsers().stream()
                .filter(candidate -> candidate.name().equals(userName))
                .findFirst().orElse(null);
        if (user == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "The session user no longer exists.");
            return;
        }
        boolean isMaker = engine.isSystemLoaded()
                && engine.getAllMarketEvents().stream()
                        .anyMatch(event -> event.marketMakerName().stream()
                                .anyMatch(userName::equals));
        HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                ViewMapper.user(user, isMaker ? Set.of(userName) : Set.of()));
    }
}
