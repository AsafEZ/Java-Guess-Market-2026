package guessmarket.server;

import com.google.gson.Gson;
import guessmarket.engine.api.GuessMarketEngine;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

final class HttpApi {
    private static final Gson GSON = new Gson();

    private HttpApi() {
    }

    static GuessMarketEngine engine(ServletContext context) {
        Object attribute = context.getAttribute(EngineContextListener.ENGINE_ATTRIBUTE);
        return attribute instanceof GuessMarketEngine engine ? engine : null;
    }

    static String sessionUserName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(LoginServlet.USER_NAME_ATTRIBUTE);
        return value instanceof String name ? name : null;
    }

    static void writeJson(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(GSON.toJson(body));
    }

    static void writeError(HttpServletResponse response, int status,
                           String code, String message) throws IOException {
        writeJson(response, status, new ApiError(code, message));
    }

    private record ApiError(String errorCode, String message) {
    }
}
