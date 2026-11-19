package vn.io.ajarline.learning.contract;

import vn.io.ajarline.content.contract.ContentRecordView;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a Module with its place in the Series and its Articles in order.
 */
public record ModuleView(
        ContentRecordView record,
        List<UUID> topicIds,
        String overviewMarkdown,
        UUID seriesId,
        int position,
        List<LearningChild> articles
) {

    public ModuleView {
        topicIds = List.copyOf(topicIds);
        articles = List.copyOf(articles);
    }
}
