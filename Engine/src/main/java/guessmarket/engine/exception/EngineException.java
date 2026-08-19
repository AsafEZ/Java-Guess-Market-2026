package guessmarket.engine.exception;

import java.util.Objects;

/** Expected business/validation failure that a UI can display without crashing. */
public class EngineException extends RuntimeException {
    private final ErrorCode errorCode;

    public EngineException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public EngineException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}