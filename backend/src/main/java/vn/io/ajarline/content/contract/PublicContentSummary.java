package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Holds the public fields of one content item that is public now.
 *
 * <p>It has no publication state, revision or creation time, so a caller
 * cannot show what the public must not see.</p>
 */
public record PublicContentSummary(
        UUID id,
        ContentType type,
        String slug,
        String title,
        String summary,
        Instant publishedAt,
        Instant updatedAt
) {
}
