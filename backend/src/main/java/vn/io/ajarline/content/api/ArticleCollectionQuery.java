package vn.io.ajarline.content.api;

import vn.io.ajarline.platform.web.ApiQueryException;
import vn.io.ajarline.platform.web.PageQuery;

/**
 * Holds the checked query of the standalone Article list.
 *
 * <p>{@code standalone} must be exactly {@code true}: there is no list of
 * every Article, so the only valid request names the standalone list.
 * {@code page} and {@code size} follow the rule of {@link PageQuery}.</p>
 */
record ArticleCollectionQuery(int page, int size) {

    private static final String STANDALONE_TOKEN = "true";

    /**
     * Checks the raw query values in the order standalone, page, size.
     *
     * @throws ApiQueryException when a value has the wrong shape
     */
    static ArticleCollectionQuery parse(String standalone, String page, String size) {
        if (!STANDALONE_TOKEN.equals(standalone)) {
            throw new ApiQueryException(
                    "INVALID_ARTICLE_COLLECTION_MODE",
                    "Article collection request is invalid",
                    "standalone must be true.");
        }
        PageQuery paging = PageQuery.parse(page, size);
        return new ArticleCollectionQuery(paging.page(), paging.size());
    }
}
