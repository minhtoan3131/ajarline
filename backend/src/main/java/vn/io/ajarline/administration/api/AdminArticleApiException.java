package vn.io.ajarline.administration.api;

public class AdminArticleApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final String title;

    AdminArticleApiException(int status, String code, String title, String detail) {
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

    static AdminArticleApiException invalidRequest(String detail) {
        return new AdminArticleApiException(
                400, "INVALID_REQUEST", "Request is invalid", detail
        );
    }

    static AdminArticleApiException invalidUuid(String field) {
        return new AdminArticleApiException(
                400, "INVALID_UUID", "UUID is invalid", field + " must be a valid UUID."
        );
    }

    static AdminArticleApiException unprocessable(String code, String title, String detail) {
        return new AdminArticleApiException(422, code, title, detail);
    }

    static AdminArticleApiException preconditionRequired() {
        return new AdminArticleApiException(
                428,
                "REVISION_PRECONDITION_REQUIRED",
                "If-Match revision precondition is required",
                "If-Match revision precondition is required.");
    }
}
