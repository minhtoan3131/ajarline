package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * One short line of the common record, for reading many items at once.
 */
public record ContentRecordSummary(
        UUID id,
        ContentType type,
        String title,
        String slug,
        String summary,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant updatedAt
) {
}
