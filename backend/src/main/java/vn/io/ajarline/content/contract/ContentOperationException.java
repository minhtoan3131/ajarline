package vn.io.ajarline.content.contract;

import java.util.Objects;

public class ContentOperationException extends RuntimeException {
    private final ContentFailureCode code;

    public ContentOperationException(ContentFailureCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ContentOperationException(ContentFailureCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ContentFailureCode code() {
        return code;
    }
}
