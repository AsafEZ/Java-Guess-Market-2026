package guessmarket.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import guessmarket.engine.api.Assignment3Engine;
import guessmarket.engine.dto.LimitOrderDetails;
import guessmarket.engine.enums.OrderSide;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.protocol.AccountActivityView;
import guessmarket.protocol.ActionResultView;
import guessmarket.protocol.OrderView;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public final class EventActionServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in before acting on an event.");
            return;
        }
        Assignment3Engine engine = HttpApi.assignment3Engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        String action = request.getServletPath();
        if (!action.equals("/api/event/open")
                && !action.equals("/api/event/purchase")
                && !action.equals("/api/event/order")
                && !action.equals("/api/event/close")) {
            HttpApi.writeError(response, HttpServletResponse.SC_NOT_FOUND,
                    "UNKNOWN_ACTION", "Unknown event action.");
            return;
        }

        JsonObject body;
        int eventId;
        try {
            var parsed = JsonParser.parseReader(request.getReader());
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Expected a JSON object.");
            }
            body = parsed.getAsJsonObject();
            eventId = positiveInt(body, "eventId");
        } catch (JsonParseException | IllegalArgumentException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", exception.getMessage());
            return;
        }

        try {
            OrderView submittedOrder = null;
            switch (action) {
                case "/api/event/open" -> engine.openEvent(eventId, userName);
                case "/api/event/purchase" -> engine.purchaseShares(eventId, userName,
                        positiveInt(body, "optionNumber"), positiveLong(body, "quantity"));
                case "/api/event/order" -> {
                    OrderSide side = OrderSide.valueOf(
                            requiredString(body, "side").toUpperCase(Locale.ROOT));
                    LimitOrderDetails order = engine.submitOrder(eventId, userName,
                            positiveInt(body, "optionNumber"), side,
                            positiveLong(body, "quantity"), positiveDouble(body, "limitPrice"))
                            .submittedOrder();
                    submittedOrder = ViewMapper.order(order);
                }
                case "/api/event/close" -> engine.closeEvent(eventId, userName,
                        positiveInt(body, "winningOptionNumber"));
                default -> throw new IllegalStateException("Unknown event action.");
            }
            List<guessmarket.engine.dto.AccountActivityDetails> history =
                    engine.getAccountHistory(userName);
            AccountActivityView activity = history.isEmpty() ? null
                    : ViewMapper.activity(history.getLast());
            HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                    new ActionResultView(action.substring(action.lastIndexOf('/') + 1),
                            ViewMapper.eventDetails(engine.getMarketEventDetails(eventId)),
                            ViewMapper.userDetails(engine.getUserDetails(userName)),
                            activity, submittedOrder));
        } catch (IllegalArgumentException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", exception.getMessage());
        } catch (EngineException exception) {
            int status = switch (exception.getErrorCode()) {
                case EVENT_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
                case USER_NOT_MARKET_MAKER -> HttpServletResponse.SC_FORBIDDEN;
                case EVENT_ALREADY_STARTED, EVENT_ALREADY_CLOSED,
                        EVENT_NOT_STARTED, INSUFFICIENT_FUNDS,
                        INSUFFICIENT_SHARES, USER_ACCOUNT_BLOCKED ->
                        HttpServletResponse.SC_CONFLICT;
                default -> HttpServletResponse.SC_BAD_REQUEST;
            };
            HttpApi.writeError(response, status,
                    exception.getErrorCode().name(), exception.getMessage());
        }
    }

    private static int positiveInt(JsonObject body, String name) {
        try {
            int value = number(body, name).intValueExact();
            if (value < 1) {
                throw new ArithmeticException();
            }
            return value;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be a positive integer.");
        }
    }

    private static long positiveLong(JsonObject body, String name) {
        try {
            long value = number(body, name).longValueExact();
            if (value < 1) {
                throw new ArithmeticException();
            }
            return value;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be a positive integer.");
        }
    }

    private static double positiveDouble(JsonObject body, String name) {
        double value = number(body, name).doubleValue();
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be a positive finite number.");
        }
        return value;
    }

    private static BigDecimal number(JsonObject body, String name) {
        if (!body.has(name) || !body.get(name).isJsonPrimitive()
                || !body.get(name).getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be numeric.");
        }
        try {
            return new BigDecimal(body.get(name).getAsString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be numeric.");
        }
    }

    private static String requiredString(JsonObject body, String name) {
        if (!body.has(name) || !body.get(name).isJsonPrimitive()
                || !body.get(name).getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(name + " must be a string.");
        }
        return body.get(name).getAsString();
    }
}
