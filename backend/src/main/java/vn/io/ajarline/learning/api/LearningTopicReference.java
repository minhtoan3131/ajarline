package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.UUID;

/**
 * Represents one Topic shown with a Series or a Module.
 */
@Schema(requiredProperties = {"id", "slug", "name"})
public record LearningTopicReference(
        UUID id,
        String slug,
        String name
) {

    static LearningTopicReference from(TopicLabel label) {
        return new LearningTopicReference(label.id(), label.slug(), label.name());
    }
}
