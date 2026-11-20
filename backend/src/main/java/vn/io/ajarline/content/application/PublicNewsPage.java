package vn.io.ajarline.content.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds one page of public News. Each entry carries every Topic assigned to the item.
 * There is no total count on purpose.
 */
public record PublicNewsPage(
        List<Entry> entries,
        boolean hasMore
) {

    public PublicNewsPage {
        entries = List.copyOf(entries);
    }

    /** Returns a page with no entries and no next page. */
    public static PublicNewsPage empty() {
        return new PublicNewsPage(List.of(), false);
    }

    /**
     * Holds one public News item and its Topics, ordered by name and then by id.
     */
    public record Entry(
            PublicContentSummary content,
            List<TopicLabel> topics
    ) {

        public Entry {
            topics = List.copyOf(topics);
        }
    }
}
