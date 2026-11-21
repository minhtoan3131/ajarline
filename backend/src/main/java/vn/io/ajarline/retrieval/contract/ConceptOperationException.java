package vn.io.ajarline.retrieval.contract;

import java.util.Objects;

/**
 * Reports a business failure of a Concept operation as a stable failure code.
 */
public class ConceptOperationException extends RuntimeException {
    private final ConceptFailureCode code;

    public ConceptOperationException(ConceptFailureCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ConceptOperationException(ConceptFailureCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ConceptFailureCode code() {
        return code;
    }
}
