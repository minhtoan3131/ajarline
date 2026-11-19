package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Holds a public Series with its overview, its Topics and its public Modules in order.
 * The list of Modules is never empty, and so is the list of Articles of each Module.
 */
public record PublicSeriesView(
        PublicContentSummary series,
        String overviewMarkdown,
        List<TopicLabel> topics,
        List<ModuleEntry> modules
) {

    public PublicSeriesView {
        topics = List.copyOf(topics);
        modules = List.copyOf(modules);
    }
}
