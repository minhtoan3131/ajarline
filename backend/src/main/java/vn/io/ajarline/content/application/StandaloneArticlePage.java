package vn.io.ajarline.content.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds one page of standalone public Articles. Each entry carries every
 * Topic assigned to the Article. There is no total count on purpose.
 */
public record StandaloneArticlePage(
        List<Entry> entries,
        boolean hasMore
) {

    public StandaloneArticlePage {
        entries = List.copyOf(entries);
    }

    /** Returns a page with no entries and no next page. */
    public static StandaloneArticlePage empty() {
        return new StandaloneArticlePage(List.of(), false);
    }

    /**
     * Holds one public Article and its Topics, ordered by name and then by id.
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
