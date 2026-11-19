package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.topic.contract.TopicUpdateInput;

import java.util.Set;

/**
 * Represents the complete editable payload of a Topic at the Admin HTTP boundary.
 * It has no slug because a Topic slug never changes.
 */
@Schema(
        description = "Complete editable Topic payload. The slug cannot be sent.",
        requiredProperties = {"name", "description", "active"}
)
public record TopicEditable(
        String name,
        String description,
        boolean active
) {

    private static final Set<String> FIELD_NAMES = Set.of("name", "description", "active");

    static TopicEditable parse(JsonNode body) {
        TopicPayloadFields.requireExactFields(body, FIELD_NAMES);
        return new TopicEditable(
                TopicPayloadFields.requiredText(body.get("name"), "name"),
                TopicPayloadFields.nullableText(body.get("description"), "description"),
                TopicPayloadFields.requiredBoolean(body.get("active"), "active"));
    }

    TopicUpdateInput toInput() {
        return new TopicUpdateInput(name, description, active);
    }
}
