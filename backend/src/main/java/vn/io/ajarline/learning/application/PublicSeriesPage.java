package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds one page of public Series. Each entry carries every Topic assigned to the Series.
 * There is no total count on purpose.
 */
public record PublicSeriesPage(
        List<Entry> entries,
        boolean hasMore
) {

    public PublicSeriesPage {
        entries = List.copyOf(entries);
    }

    /** Returns a page with no entries and no next page. */
    public static PublicSeriesPage empty() {
        return new PublicSeriesPage(List.of(), false);
    }

    /**
     * Holds one public Series and its Topics, ordered by name and then by id.
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
