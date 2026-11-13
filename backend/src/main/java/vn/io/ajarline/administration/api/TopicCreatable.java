package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.topic.contract.TopicCreateInput;

import java.util.Set;

/**
 * Represents the complete payload for creating a Topic at the Admin HTTP boundary.
 */
@Schema(
        description = "Complete payload for creating a Topic. The slug cannot be changed later.",
        requiredProperties = {"slug", "name", "description", "active"}
)
public record TopicCreatable(
        String slug,
        String name,
        String description,
        boolean active
) {

    private static final Set<String> FIELD_NAMES = Set.of("slug", "name", "description", "active");

    static TopicCreatable parse(JsonNode body) {
        TopicPayloadFields.requireExactFields(body, FIELD_NAMES);
        return new TopicCreatable(
                TopicPayloadFields.requiredText(body.get("slug"), "slug"),
                TopicPayloadFields.requiredText(body.get("name"), "name"),
                TopicPayloadFields.nullableText(body.get("description"), "description"),
                TopicPayloadFields.requiredBoolean(body.get("active"), "active"));
    }

    TopicCreateInput toInput() {
        return new TopicCreateInput(slug, name, description, active);
    }
}
