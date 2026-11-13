package vn.io.ajarline.publication.contract;

import java.util.Objects;

public class PublicationOperationException extends RuntimeException {

    private final PublicationFailureCode code;

    public PublicationOperationException(PublicationFailureCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public PublicationFailureCode code() {
        return code;
    }
}
