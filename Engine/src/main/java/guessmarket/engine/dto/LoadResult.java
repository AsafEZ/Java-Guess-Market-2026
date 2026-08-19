package guessmarket.engine.dto;

import java.nio.file.Path;
import java.util.Objects;

public record LoadResult(Path sourcePath, int loadedEventCount, double totalInitialSubsidy) {
    public LoadResult {
        Objects.requireNonNull(sourcePath, "sourcePath");
    }
}