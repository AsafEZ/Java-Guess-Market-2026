package guessmarket.client;

import javafx.application.Application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ClientLauncher {
    private ClientLauncher() {
    }

    public static void main(String[] args) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .contains("windows")
                && System.getProperty("jdk.net.unixdomain.tmpdir") == null) {
            String publicDirectory = System.getenv("PUBLIC");
            if (publicDirectory != null && Files.isDirectory(Path.of(publicDirectory))) {
                System.setProperty("jdk.net.unixdomain.tmpdir", publicDirectory);
            }
        }
        Application.launch(MarketClientApplication.class, args);
    }
}
