package vn.io.ajarline.topic.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.topic.application.PublicTopicView;

import java.util.UUID;

/**
 * Represents one public Topic. The active flag is never part of it.
 */
@Schema(requiredProperties = {"id", "slug", "name", "description"})
public record PublicTopicResponse(
        UUID id,
        String slug,
        String name,
        @Schema(nullable = true) String description
) {
    static PublicTopicResponse from(PublicTopicView view) {
        return new PublicTopicResponse(view.id(), view.slug(), view.name(), view.description());
    }
}
