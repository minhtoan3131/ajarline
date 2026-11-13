package vn.io.ajarline.topic.contract;

import java.util.Objects;

/**
 * Reports a business failure of a Topic operation as a stable failure code.
 */
public class TopicOperationException extends RuntimeException {
    private final TopicFailureCode code;

    public TopicOperationException(TopicFailureCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public TopicOperationException(TopicFailureCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public TopicFailureCode code() {
        return code;
    }
}
