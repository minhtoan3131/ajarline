package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RevisionHttpHeaders {

    private static final Pattern STRONG_DECIMAL = Pattern.compile("^\"([0-9]+)\"$");

    private RevisionHttpHeaders() {

    }

    /**
     * Converts an Article revision into an HTTP ETag.
     *
     * @param revision the Article revision
     * @return the ETag with double quotes
     */
    static String etag(long revision) {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        return "\"" + revision + "\"";
    }

    /**
     * Reads the If-Match header and returns the expected Article revision.
     * Requires exactly one valid, positive revision.
     *
     * @param request the HTTP request
     * @return the expected Article revision
     * @throws AdminApiException if the If-Match header is missing or invalid
     */
    static long requiredExpectedRevision(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(HttpHeaders.IF_MATCH));
        if (values.isEmpty()) {
            throw AdminApiException.preconditionRequired();
        }
        if (values.size() != 1) {
            throw invalidIfMatch();
        }
        Matcher matcher = STRONG_DECIMAL.matcher(values.getFirst());
        if (!matcher.matches()) {
            throw invalidIfMatch();
        }
        try {
            long revision = Long.parseLong(matcher.group(1));
            if (revision < 1) {
                throw invalidIfMatch();
            }
            return revision;
        } catch (NumberFormatException exception) {
            throw invalidIfMatch();
        }
    }

    private static AdminApiException invalidIfMatch() {
        return AdminApiException.invalidRequest("If-Match must contain exactly one strong decimal ETag.");
    }
}
