package vn.io.ajarline.content.contract;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a News item: the common record, the body, the source and the Topic ids.
 */
public record NewsView(
        ContentRecordView record,
        String bodyMarkdown,
        NewsSource source,
        List<UUID> topicIds
) {

    public NewsView {
        topicIds = List.copyOf(topicIds);
    }
}
