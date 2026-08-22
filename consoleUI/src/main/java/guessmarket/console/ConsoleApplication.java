package guessmarket.console;

import guessmarket.engine.api.EngineFactory;
import guessmarket.engine.api.GuessMarketEngine;

import java.util.Scanner;


public final class ConsoleApplication {

    private ConsoleApplication() {
    }

    public static void start() {
        GuessMarketEngine engine = EngineFactory.createEngine();
        ConsoleView view = new ConsoleView(System.out);

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInput input = new ConsoleInput(scanner, System.out);
            ConsoleController controller = new ConsoleController(engine, input, view);
            controller.run();
        }
    }
}
