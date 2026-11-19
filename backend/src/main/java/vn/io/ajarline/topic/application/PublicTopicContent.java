package vn.io.ajarline.topic.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds one page of the public content directly assigned to a Topic. Each
 * item carries every Topic assigned to it, not only the browsed one.
 */
public record PublicTopicContent(
        List<Entry> items,
        boolean hasMore
) {

    public PublicTopicContent {
        items = List.copyOf(items);
    }

    /**
     * Holds one public content item and its Topics, ordered by name and then
     * by id.
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
