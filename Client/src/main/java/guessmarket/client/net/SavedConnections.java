package guessmarket.client.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

final class SavedConnections {
    private final Path directory;

    SavedConnections(Path directory) {
        this.directory = directory;
    }

    List<String> userNames() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".token"))
                    .toList()) {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (lines.size() == 2 && !lines.get(0).isBlank()) {
                    names.add(lines.get(0));
                }
            }
        } catch (IOException exception) {
            return List.of();
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(names);
    }

    String token(String userName) throws IOException {
        List<String> lines = Files.readAllLines(path(userName), StandardCharsets.UTF_8);
        if (lines.size() != 2 || !userName.equals(lines.get(0)) || lines.get(1).isBlank()) {
            throw new IOException("Saved connection is invalid.");
        }
        return lines.get(1);
    }

    void save(String userName, String token) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "connection-", ".tmp");
        try {
            Files.writeString(temporary, userName + "\n" + token + "\n",
                    StandardCharsets.UTF_8);
            Files.move(temporary, path(userName), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void forget(String userName) throws IOException {
        Files.deleteIfExists(path(userName));
    }

    private Path path(String userName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userName.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(digest) + ".token");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
