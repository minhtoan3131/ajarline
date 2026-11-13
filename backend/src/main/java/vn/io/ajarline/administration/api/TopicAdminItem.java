package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.topic.contract.TopicView;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents one Topic returned by the Admin API, including inactive Topics.
 */
@Schema(requiredProperties = {
        "id", "slug", "name", "description", "active", "createdAt", "updatedAt"
})
public record TopicAdminItem(
        UUID id,
        String slug,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    static TopicAdminItem from(TopicView view) {
        return new TopicAdminItem(
                view.id(),
                view.slug(),
                view.name(),
                view.description(),
                view.active(),
                view.createdAt(),
                view.updatedAt());
    }
}
