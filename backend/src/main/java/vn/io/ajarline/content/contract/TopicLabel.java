package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Holds the public identity of one Topic shown with content.
 */
public record TopicLabel(
        UUID id,
        String slug,
        String name
) {
}
