package guessmarket.server;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.GuessMarketEngine;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public final class EngineContextListener implements ServletContextListener {
    static final String ENGINE_ATTRIBUTE = GuessMarketEngine.class.getName();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(
                ENGINE_ATTRIBUTE, EngineFactory.createAssignment3Engine());
        event.getServletContext().setAttribute(
                ResumeSessions.CONTEXT_ATTRIBUTE, new ResumeSessions());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ENGINE_ATTRIBUTE);
        event.getServletContext().removeAttribute(ResumeSessions.CONTEXT_ATTRIBUTE);
    }
}
