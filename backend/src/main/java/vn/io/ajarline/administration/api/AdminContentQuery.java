package vn.io.ajarline.administration.api;

import vn.io.ajarline.content.contract.ContentIndexFilter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.platform.web.ApiQueryException;
import vn.io.ajarline.platform.web.PageQuery;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Holds the checked query of the Admin content list.
 *
 * <p>{@code types} and {@code statuses} are comma-separated lists of known names with no
 * repeats. Each name is trimmed. {@code q} is trimmed and has 1 to 300 characters when it
 * is given. {@code page} and {@code size} follow {@link PageQuery}, with 100 as the
 * largest size.</p>
 */
record AdminContentQuery(
        List<String> typeTokens,
        List<String> statusTokens,
        String q,
        int page,
        int size
) {
    static final List<String> TYPE_TOKENS = List.of("series", "module", "article", "news");

    private static final List<String> STATUS_TOKENS =
            Arrays.stream(PublicationStatus.values()).map(Enum::name).toList();
    private static final int MAX_SIZE = 100;
    private static final int MAX_Q_LENGTH = 300;
    private static final String TITLE = "Admin content-index query is invalid";

    AdminContentQuery {
        typeTokens = List.copyOf(typeTokens);
        statusTokens = List.copyOf(statusTokens);
    }

    /**
     * Checks the raw values in the order types, statuses, q, page, size.
     *
     * @throws ApiQueryException when a value has the wrong shape
     */
    static AdminContentQuery parse(String types, String statuses, String q, String page, String size) {
        List<String> typeTokens = parseTokens(
                types, TYPE_TOKENS, "INVALID_RESULT_TYPE", "types must be known type names without repeats.");
        List<String> statusTokens = parseTokens(
                statuses, STATUS_TOKENS, "INVALID_REQUEST", "statuses must be known status names without repeats.");
        String text = parseText(q);
        PageQuery paging = PageQuery.parse(page, size, MAX_SIZE);
        return new AdminContentQuery(typeTokens, statusTokens, text, paging.page(), paging.size());
    }

    /**
     * Returns the filter for the content module. No token means every type, or every status.
     */
    ContentIndexFilter toFilter() {
        Set<ContentType> types = EnumSet.noneOf(ContentType.class);
        for (ContentType type : ContentType.values()) {
            if (typeTokens.isEmpty() || typeTokens.contains(wireName(type))) {
                types.add(type);
            }
        }
        Set<PublicationStatus> statuses = EnumSet.noneOf(PublicationStatus.class);
        for (PublicationStatus status : PublicationStatus.values()) {
            if (statusTokens.isEmpty() || statusTokens.contains(status.name())) {
                statuses.add(status);
            }
        }
        return new ContentIndexFilter(types, statuses, q);
    }

    /** Returns the lowercase name used for a content type on the wire. */
    static String wireName(ContentType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static List<String> parseTokens(
            String raw, List<String> allowed, String code, String detail) {
        if (raw == null) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        // The limit -1 keeps empty parts, so "series," is rejected.
        for (String part : raw.split(",", -1)) {
            String token = part.strip();
            if (!allowed.contains(token) || !tokens.add(token)) {
                throw new ApiQueryException(code, TITLE, detail);
            }
        }
        return List.copyOf(tokens);
    }

    private static String parseText(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.strip();
        if (text.isEmpty() || text.codePointCount(0, text.length()) > MAX_Q_LENGTH) {
            throw new ApiQueryException(
                    "INVALID_REQUEST", TITLE, "q must have 1 to " + MAX_Q_LENGTH + " characters.");
        }
        return text;
    }
}
