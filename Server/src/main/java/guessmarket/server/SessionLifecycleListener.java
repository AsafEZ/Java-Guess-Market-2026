package guessmarket.server;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public final class SessionLifecycleListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Object attribute = event.getSession().getServletContext()
                .getAttribute(ResumeSessions.CONTEXT_ATTRIBUTE);
        if (attribute instanceof ResumeSessions sessions) {
            sessions.sessionEnded(event.getSession());
        }
    }
}
