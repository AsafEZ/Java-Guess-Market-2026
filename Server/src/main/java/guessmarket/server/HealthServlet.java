package guessmarket.server;

import com.google.gson.Gson;
import guessmarket.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class HealthServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Object attribute = getServletContext().getAttribute(
                EngineContextListener.ENGINE_ATTRIBUTE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (!(attribute instanceof GuessMarketEngine engine)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write(GSON.toJson(new HealthResponse("unavailable", false)));
            return;
        }
        response.getWriter().write(GSON.toJson(
                new HealthResponse("ok", engine.isSystemLoaded())));
    }

    private record HealthResponse(String status, boolean systemLoaded) {
    }
}
