package vn.io.ajarline.content.contract;

import java.util.List;

/**
 * One page of the content index. {@code hasMore} tells whether a next page exists.
 * There is no total count on purpose.
 */
public record ContentIndexPage(
        List<ContentRecordSummary> items,
        boolean hasMore
) {

    public ContentIndexPage {
        items = List.copyOf(items);
    }

    /** Returns a page with no items and no next page. */
    public static ContentIndexPage empty() {
        return new ContentIndexPage(List.of(), false);
    }
}
