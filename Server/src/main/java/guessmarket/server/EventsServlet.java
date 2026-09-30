package guessmarket.server;

import guessmarket.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public final class EventsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (HttpApi.sessionUserName(request) == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to view events.");
            return;
        }
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        List<guessmarket.protocol.EventView> events = engine.isSystemLoaded()
                ? engine.getAllMarketEvents().stream().map(ViewMapper::event).toList()
                : List.of();
        HttpApi.writeJson(response, HttpServletResponse.SC_OK, events);
    }
}
