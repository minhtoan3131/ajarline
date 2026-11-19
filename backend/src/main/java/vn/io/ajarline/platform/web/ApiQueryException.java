package vn.io.ajarline.platform.web;

/**
 * Reports a query parameter whose shape is wrong, such as a page that is not
 * a number.
 *
 * <p>The error only has a meaning at the HTTP layer and is the same for every
 * endpoint, so one type is shared by all capabilities. It always maps to
 * status 400. The {@code code} is part of the API contract; the {@code title}
 * is not.</p>
 */
public final class ApiQueryException extends RuntimeException {

    private final String code;
    private final String title;

    /**
     * Creates the error.
     *
     * @param code the stable error code of the API contract
     * @param title a short summary of the problem
     * @param detail the explanation that names the bad parameter
     */
    public ApiQueryException(String code, String title, String detail) {
        super(detail);
        this.code = code;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
