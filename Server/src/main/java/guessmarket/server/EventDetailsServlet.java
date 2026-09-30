package guessmarket.server;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class EventDetailsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (HttpApi.sessionUserName(request) == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to view event details.");
            return;
        }
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
            if (id < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            HttpApi.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "INVALID_REQUEST", "Event id must be a positive integer.");
            return;
        }
        try {
            HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                    ViewMapper.eventDetails(engine.getMarketEventDetails(id)));
        } catch (EngineException exception) {
            int status = exception.getErrorCode() == ErrorCode.EVENT_NOT_FOUND
                    ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            HttpApi.writeError(response, status,
                    exception.getErrorCode().name(), exception.getMessage());
        }
    }
}
