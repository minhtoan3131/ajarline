package vn.io.ajarline.learning.contract;

import java.util.Objects;

/**
 * Reports a business failure of a learning operation as a stable failure code.
 */
public class LearningOperationException extends RuntimeException {
    private final LearningFailureCode code;

    public LearningOperationException(LearningFailureCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public LearningFailureCode code() {
        return code;
    }
}
