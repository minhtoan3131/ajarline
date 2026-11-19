package vn.io.ajarline.learning.contract;

import vn.io.ajarline.content.contract.ContentRecordView;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a Series with its Modules in order.
 */
public record SeriesView(
        ContentRecordView record,
        List<UUID> topicIds,
        String overviewMarkdown,
        List<LearningChild> modules
) {

    public SeriesView {
        topicIds = List.copyOf(topicIds);
        modules = List.copyOf(modules);
    }
}
