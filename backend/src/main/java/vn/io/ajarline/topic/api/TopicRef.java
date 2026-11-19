package vn.io.ajarline.topic.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.UUID;

/**
 * Represents the public identity of one Topic inside a public response.
 */
@Schema(requiredProperties = {"id", "slug", "name"})
public record TopicRef(
        UUID id,
        String slug,
        String name
) {
    static TopicRef from(TopicLabel label) {
        return new TopicRef(label.id(), label.slug(), label.name());
    }
}
