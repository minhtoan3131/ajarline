package vn.io.ajarline.topic.api;

/**
 * Reports a query parameter of a public Topic request that has the wrong
 * shape. It always maps to status 400.
 */
final class PublicTopicQueryException extends RuntimeException {
    private final String code;
    private final String title;

    PublicTopicQueryException(String code, String title, String detail) {
        super(detail);
        this.code = code;
        this.title = title;
    }

    String code() {
        return code;
    }

    String title() {
        return title;
    }
}
