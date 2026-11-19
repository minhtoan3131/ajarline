package vn.io.ajarline.content.contract;

import java.util.List;

/**
 * Holds one page of public content items.
 *
 * <p>{@code hasMore} tells whether a next page exists. There is no total
 * count on purpose.</p>
 */
public record PublicContentPage(
        List<PublicContentSummary> items,
        boolean hasMore
) {

    public PublicContentPage {
        items = List.copyOf(items);
    }

    /** Returns a page with no items and no next page. */
    public static PublicContentPage empty() {
        return new PublicContentPage(List.of(), false);
    }
}
