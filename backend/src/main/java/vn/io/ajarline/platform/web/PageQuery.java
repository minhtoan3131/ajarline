package vn.io.ajarline.platform.web;

/**
 * Holds the checked paging values of a collection request.
 *
 * <p>{@code page} starts at 0. {@code size} is 1 to a maximum, and {@value #DEFAULT_SIZE}
 * when it is not given. Public collections use {@value #MAX_SIZE} as the maximum. A caller
 * with another limit passes it in. The rule is kept in this one place.</p>
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private static final String TITLE = "Collection request is invalid";

    /**
     * Checks the raw {@code page} and {@code size} values with the public maximum size.
     *
     * @throws ApiQueryException when a value is not an integer in range
     */
    public static PageQuery parse(String page, String size) {
        return parse(page, size, MAX_SIZE);
    }

    /**
     * Checks the raw values. The page is checked before the size, and an absent value
     * gets its default.
     *
     * @param maxSize the largest allowed size
     * @throws ApiQueryException when a value is not an integer in range
     */
    public static PageQuery parse(String page, String size, int maxSize) {
        return new PageQuery(parsePage(page), parseSize(size, maxSize));
    }

    private static int parsePage(String raw) {
        if (raw == null) {
            return 0;
        }
        Integer page = parseInteger(raw);
        if (page == null || page < 0) {
            throw new ApiQueryException(
                    "INVALID_PAGE",
                    TITLE,
                    "page must be an integer of at least 0.");
        }
        return page;
    }

    private static int parseSize(String raw, int maxSize) {
        if (raw == null) {
            return DEFAULT_SIZE;
        }
        Integer size = parseInteger(raw);
        if (size == null || size < 1 || size > maxSize) {
            throw new ApiQueryException(
                    "INVALID_PAGE_SIZE",
                    TITLE,
                    "size must be an integer from 1 to " + maxSize + ".");
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
