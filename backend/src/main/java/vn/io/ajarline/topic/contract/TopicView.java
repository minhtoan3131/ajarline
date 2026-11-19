package vn.io.ajarline.topic.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Holds a snapshot of one Topic returned by the Topic capability.
 *
 * <p>This is an immutable output object. It is not a database entity and it is
 * not an HTTP response.</p>
 */
public record TopicView(
        UUID id,
        String slug,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
