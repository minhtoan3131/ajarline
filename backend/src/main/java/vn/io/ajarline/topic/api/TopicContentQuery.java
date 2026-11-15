package vn.io.ajarline.topic.api;

import vn.io.ajarline.content.contract.ContentType;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Holds the checked query of the Topic content list.
 *
 * <p>{@code types} is a comma-separated list of known type tokens with no
 * repeats; each token is trimmed. {@code page} starts at 0. {@code size} is
 * 1 to 50, 20 by default.</p>
 */
record TopicContentQuery(
        List<String> typeTokens,
        Set<ContentType> contentTypes,
        int page,
        int size
) {
    static final List<String> TYPE_TOKENS = List.of("series", "module", "article", "news");
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;

    TopicContentQuery {
        typeTokens = List.copyOf(typeTokens);
        contentTypes = Set.copyOf(contentTypes);
    }

    /**
     * Checks the raw query values.
     *
     * @throws PublicTopicQueryException when a value has the wrong shape
     */
    static TopicContentQuery parse(String types, String page, String size) {
        List<String> tokens = parseTypes(types);
        return new TopicContentQuery(tokens, contentTypes(tokens), parsePage(page), parseSize(size));
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
                throw new PublicTopicQueryException(
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

    private static int parsePage(String raw) {
        if (raw == null) {
            return 0;
        }
        Integer page = parseInteger(raw);
        if (page == null || page < 0) {
            throw new PublicTopicQueryException(
                    "INVALID_PAGE",
                    "Collection request is invalid",
                    "page must be an integer of at least 0.");
        }
        return page;
    }

    private static int parseSize(String raw) {
        if (raw == null) {
            return DEFAULT_SIZE;
        }
        Integer size = parseInteger(raw);
        if (size == null || size < 1 || size > MAX_SIZE) {
            throw new PublicTopicQueryException(
                    "INVALID_PAGE_SIZE",
                    "Collection request is invalid",
                    "size must be an integer from 1 to " + MAX_SIZE + ".");
        }
        return size;
    }

    private static Integer parseInteger(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
