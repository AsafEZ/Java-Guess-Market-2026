package guessmarket.server;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.UserSummary;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

public final class ResumeServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        ResumeSessions sessions = HttpApi.resumeSessions(getServletContext());
        if (engine == null || sessions == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market server is unavailable.");
            return;
        }
        if (HttpApi.sessionUserName(request) != null) {
            HttpApi.writeError(response, HttpServletResponse.SC_CONFLICT,
                    "ALREADY_LOGGED_IN", "This session is already logged in.");
            return;
        }
        String token = request.getHeader("X-Resume-Token");
        if (token == null || !sessions.contains(token)) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_RESUME_TOKEN", "This saved connection is no longer available.");
            return;
        }
        if (sessions.isActive(token)) {
            HttpApi.writeError(response, HttpServletResponse.SC_CONFLICT,
                    "USER_ALREADY_CONNECTED", "This user is already connected.");
            return;
        }
        HttpSession session = request.getSession(true);
        String userName = sessions.resume(token, session);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_CONFLICT,
                    "USER_ALREADY_CONNECTED", "This user is already connected.");
            return;
        }
        UserSummary user = engine.getAllUsers().stream()
                .filter(candidate -> candidate.name().equals(userName))
                .findFirst().orElse(null);
        if (user == null) {
            session.invalidate();
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_RESUME_TOKEN", "This saved connection is no longer available.");
            return;
        }
        session.setAttribute(LoginServlet.USER_NAME_ATTRIBUTE, userName);
        HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                ViewMapper.user(user, Set.of()));
    }
}
