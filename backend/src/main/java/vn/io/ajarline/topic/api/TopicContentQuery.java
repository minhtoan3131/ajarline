package vn.io.ajarline.topic.api;

import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.platform.web.ApiQueryException;
import vn.io.ajarline.platform.web.PageQuery;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Holds the checked query of the Topic content list.
 *
 * <p>{@code types} is a comma-separated list of known type tokens with no
 * repeats; each token is trimmed. {@code page} and {@code size} follow the rule
 * of {@link PageQuery}.</p>
 */
record TopicContentQuery(
        List<String> typeTokens,
        Set<ContentType> contentTypes,
        int page,
        int size
) {
    static final List<String> TYPE_TOKENS = List.of("series", "module", "article", "news");

    TopicContentQuery {
        typeTokens = List.copyOf(typeTokens);
        contentTypes = Set.copyOf(contentTypes);
    }

    /**
     * Checks the raw query values. The types are checked before the paging.
     *
     * @throws ApiQueryException when a value has the wrong shape
     */
    static TopicContentQuery parse(String types, String page, String size) {
        List<String> tokens = parseTypes(types);
        PageQuery paging = PageQuery.parse(page, size);
        return new TopicContentQuery(tokens, contentTypes(tokens), paging.page(), paging.size());
    }

    /** Returns the lowercase name used for a content type on the wire. */
    static String wireName(ContentType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static List<String> parseTypes(String raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        // The limit -1 keeps empty parts, so "article," is rejected.
        for (String part : raw.split(",", -1)) {
            String token = part.strip();
            if (!TYPE_TOKENS.contains(token) || !tokens.add(token)) {
                throw new ApiQueryException(
                        "INVALID_RESULT_TYPE",
                        "Invalid result type",
                        "One or more Topic content types are invalid.");
            }
        }
        return List.copyOf(tokens);
    }

    /**
     * Returns the content types the tokens select. No token means every type.
     * A token with no matching content type selects no content.
     */
    private static Set<ContentType> contentTypes(List<String> tokens) {
        Set<ContentType> selected = EnumSet.noneOf(ContentType.class);
        for (ContentType type : ContentType.values()) {
            if (tokens.isEmpty() || tokens.contains(wireName(type))) {
                selected.add(type);
            }
        }
        return selected;
    }
}
