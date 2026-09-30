package guessmarket.server;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.protocol.UserView;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class UsersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (HttpApi.sessionUserName(request) == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in to view users.");
            return;
        }
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }
        Set<String> makers = engine.isSystemLoaded()
                ? engine.getAllMarketEvents().stream()
                        .flatMap(event -> event.marketMakerName().stream())
                        .collect(Collectors.toSet())
                : Set.of();
        List<UserView> users = engine.getAllUsers().stream()
                .map(user -> ViewMapper.user(user, makers)).toList();
        HttpApi.writeJson(response, HttpServletResponse.SC_OK, users);
    }
}
