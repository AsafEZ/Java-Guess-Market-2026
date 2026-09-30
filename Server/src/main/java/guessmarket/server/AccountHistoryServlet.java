package guessmarket.server;

import guessmarket.engine.api.Assignment3Engine;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class AccountHistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to view your account history.");
            return;
        }
        Assignment3Engine engine = HttpApi.assignment3Engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                engine.getAccountHistory(userName).stream()
                        .map(ViewMapper::activity).toList());
    }
}
