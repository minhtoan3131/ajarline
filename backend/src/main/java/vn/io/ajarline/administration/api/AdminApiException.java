package vn.io.ajarline.administration.api;

public class AdminApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final String title;

    AdminApiException(int status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    int status() {
        return this.status;
    }

    String code() {
        return this.code;
    }

    String title() {
        return this.title;
    }

    static AdminApiException invalidRequest(String detail) {
        return new AdminApiException(
                400, "INVALID_REQUEST", "Request is invalid", detail
        );
    }

    static AdminApiException invalidUuid(String field) {
        return new AdminApiException(
                400, "INVALID_UUID", "UUID is invalid", field + " must be a valid UUID."
        );
    }

    static AdminApiException unprocessable(String code, String title, String detail) {
        return new AdminApiException(422, code, title, detail);
    }

    static AdminApiException preconditionRequired() {
        return new AdminApiException(
                428,
                "REVISION_PRECONDITION_REQUIRED",
                "If-Match revision precondition is required",
                "If-Match revision precondition is required.");
    }
}
