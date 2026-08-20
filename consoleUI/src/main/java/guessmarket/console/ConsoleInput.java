package guessmarket.console;

import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner){
        this.scanner = scanner;
    }
    public String readLine(String message){
        System.out.print(message);
        return scanner.nextLine().trim();
    }
    public int readInt(String message) {
        while (true) {
            String value = readLine(message);

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println(
                        "Please enter a whole number.");
            }
        }
    }
    public long readLong(String message) {
        while (true) {
            String value = readLine(message);

            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                System.out.println(
                        "Please enter a whole number.");
            }
        }
    }


}
