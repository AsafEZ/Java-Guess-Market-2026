package guessmarket.engine.api;

import guessmarket.engine.impl.GuessMarketEngineImpl;

/** Keeps callers independent of the concrete implementation class. */
public final class EngineFactory {
    private EngineFactory() {
    }

    public static GuessMarketEngine createEngine() {
        return new GuessMarketEngineImpl();
    }
}