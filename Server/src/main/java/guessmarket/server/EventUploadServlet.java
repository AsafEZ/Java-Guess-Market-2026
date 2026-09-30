package guessmarket.server;

import guessmarket.engine.api.GuessMarketEngine;
import guessmarket.engine.dto.EventUploadResult;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

public final class EventUploadServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = HttpApi.sessionUserName(request);
        if (userName == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "NOT_LOGGED_IN", "Log in before uploading events.");
            return;
        }
        GuessMarketEngine engine = HttpApi.engine(getServletContext());
        if (engine == null) {
            HttpApi.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "ENGINE_UNAVAILABLE", "The market engine is unavailable.");
            return;
        }

        try (InputStream xml = request.getInputStream()) {
            EventUploadResult result = engine.uploadEvents(xml, userName);
            HttpApi.writeJson(response, HttpServletResponse.SC_OK,
                    ViewMapper.upload(result));
        } catch (EngineException exception) {
            int status = exception.getErrorCode() == ErrorCode.DUPLICATE_EVENT_NAME
                    ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST;
            HttpApi.writeError(response, status,
                    exception.getErrorCode().name(), exception.getMessage());
        }
    }
}
