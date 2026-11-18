package vn.io.ajarline.content.contract;

import java.util.List;
import java.util.UUID;

/**
 * Editable fields that every content type shares. A type with more fields wraps this record.
 */
public record ContentRecordInput(
        String title,
        String slug,
        String summary,
        List<UUID> topicIds
) {

    public ContentRecordInput {
        topicIds = List.copyOf(topicIds);
    }
}
