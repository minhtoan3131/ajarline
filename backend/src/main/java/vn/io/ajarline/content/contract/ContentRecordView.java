package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Holds a snapshot of the state that every content type shares.
 *
 * <p>This is an immutable output object, not a database entity or an HTTP
 * response. It has the fields of the common content record only, so it can
 * describe any content type. Text that belongs to one type, such as an Article
 * body, is not part of it.</p>
 */
public record ContentRecordView(
        UUID id,
        ContentType type,
        String title,
        String slug,
        String summary,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
