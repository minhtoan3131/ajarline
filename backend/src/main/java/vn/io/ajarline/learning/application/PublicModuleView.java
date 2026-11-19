package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicSeriesRef;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds a public Module with its overview, its Topics and its public Articles in order.
 * The Series is null when readers cannot see it. The list of Articles is never empty.
 */
public record PublicModuleView(
        PublicContentSummary module,
        String overviewMarkdown,
        List<TopicLabel> topics,
        PublicSeriesRef series,
        List<ArticleEntry> articles
) {

    public PublicModuleView {
        topics = List.copyOf(topics);
        articles = List.copyOf(articles);
    }
}
