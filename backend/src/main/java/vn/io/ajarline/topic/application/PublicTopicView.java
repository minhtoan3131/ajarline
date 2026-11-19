package vn.io.ajarline.topic.application;

import java.util.UUID;

/**
 * Holds the public fields of a Topic. It has no active flag because the
 * public never sees it.
 */
public record PublicTopicView(
        UUID id,
        String slug,
        String name,
        String description
) {
}
