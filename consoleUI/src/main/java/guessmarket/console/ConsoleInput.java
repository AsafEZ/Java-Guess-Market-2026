package guessmarket.console;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

/**
 * Reads raw Console input and performs syntactic validation only.
 * Business rules remain the responsibility of the Engine.
 */
public final class ConsoleInput {

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleInput(Scanner scanner, PrintStream out) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.out = Objects.requireNonNull(out, "out");
    }

    public boolean hasMoreInput() {
        return scanner.hasNextLine();
    }

    public int readInt(String prompt) {
        while (true) {
            String value = readRequiredLine(prompt);

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                out.println("Invalid input. Please enter a whole number.");

            }
        }
    }

    public int readIntInRange(String prompt, int minimum, int maximum) {
        while (true) {
            int value = readInt(prompt);

            if (value >= minimum && value <= maximum) {
                return value;
            }

            out.printf(
                    "Invalid choice. Please enter a number between %d and %d.%n",
                    minimum,
                    maximum
            );
        }
    }

    public long readPositiveLong(String prompt) {
        while (true) {
            String value = readRequiredLine(prompt);

            try {
                long number = Long.parseLong(value);

                if (number > 0) {
                    return number;
                }

                out.println("Invalid value. Please enter a positive whole number.");
            } catch (NumberFormatException exception) {
                out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    public Path readPath(String prompt) {
        while (true) {
            String value = removeSurroundingQuotes(readRequiredLine(prompt));

            try {
                return Path.of(value);
            } catch (InvalidPathException exception) {
                out.println("The entered path is invalid. Please enter a valid XML path.");
            }
        }
    }

    private String readRequiredLine(String prompt) {
        while (true) {
            out.print(prompt);

            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("The input stream was closed.");
            }

            String value = scanner.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            out.println("Input cannot be empty. Please try again.");
        }
    }

    private String removeSurroundingQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).trim();
        }

        return value;
    }
}
