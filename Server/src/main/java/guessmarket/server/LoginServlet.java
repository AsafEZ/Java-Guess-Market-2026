package guessmarket.server;

import com.google.gson.Gson;
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

public final class LoginServlet extends HttpServlet {
    static final String USER_NAME_ATTRIBUTE = "guessmarket.userName";
    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJsonResponse(response);
        GuessMarketEngine engine = engine(response);
        if (engine == null) {
            return;
        }
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(USER_NAME_ATTRIBUTE) != null) {
            writeError(response, HttpServletResponse.SC_CONFLICT,
                    "ALREADY_LOGGED_IN", "This session is already logged in.");
            return;
        }

        String userName;
        try {
            JsonElement body = JsonParser.parseReader(request.getReader());
            if (!body.isJsonObject() || !body.getAsJsonObject().has("userName")
                    || !body.getAsJsonObject().get("userName").isJsonPrimitive()
                    || !body.getAsJsonObject().get("userName").getAsJsonPrimitive().isString()) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "INVALID_REQUEST", "Expected a JSON object with a userName string.");
                return;
            }
            userName = body.getAsJsonObject().get("userName").getAsString();
        } catch (JsonParseException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", "The request body is not valid JSON.");
            return;
        }

        HttpSession session = request.getSession(true);
        try {
            UserSummary user = engine.registerUser(userName);
            session.setAttribute(USER_NAME_ATTRIBUTE, user.name());
            response.getWriter().write(GSON.toJson(user));
        } catch (EngineException exception) {
            int status = exception.getErrorCode() == ErrorCode.DUPLICATE_USER_NAME
                    ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST;
            writeError(response, status, exception.getErrorCode().name(), exception.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJsonResponse(response);
        GuessMarketEngine engine = engine(response);
        if (engine == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        String userName = session == null ? null
                : (String) session.getAttribute(USER_NAME_ATTRIBUTE);
        if (userName == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "No user is logged in for this session.");
            return;
        }
        UserSummary user = engine.getAllUsers().stream()
                .filter(candidate -> candidate.name().equals(userName))
                .findFirst().orElse(null);
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "The session user no longer exists.");
            return;
        }
        response.getWriter().write(GSON.toJson(user));
    }

    private GuessMarketEngine engine(HttpServletResponse response) throws IOException {
        Object attribute = getServletContext().getAttribute(
                EngineContextListener.ENGINE_ATTRIBUTE);
        if (attribute instanceof GuessMarketEngine engine) {
            return engine;
        }
        writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
        return null;
    }

    private static void prepareJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private static void writeError(HttpServletResponse response, int status,
                                   String code, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(GSON.toJson(new ApiError(code, message)));
    }

    private record ApiError(String errorCode, String message) {
    }
}
