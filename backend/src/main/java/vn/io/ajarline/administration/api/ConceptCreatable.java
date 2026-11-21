package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;

import java.util.List;
import java.util.Set;

/**
 * Represents the complete payload for creating a Concept at the Admin HTTP boundary.
 */
@Schema(
        description = "Complete payload for creating a Concept. The key cannot be changed later.",
        requiredProperties = {"key", "displayName", "description", "active", "expressions"}
)
public record ConceptCreatable(
        String key,
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionPayload> expressions
) {

    private static final Set<String> FIELD_NAMES =
            Set.of("key", "displayName", "description", "active", "expressions");

    static ConceptCreatable parse(JsonNode body) {
        ConceptPayloadFields.requireExactFields(body, FIELD_NAMES, "Request body");
        return new ConceptCreatable(
                ConceptPayloadFields.requiredText(body.get("key"), "key"),
                ConceptPayloadFields.requiredText(body.get("displayName"), "displayName"),
                ConceptPayloadFields.nullableText(body.get("description"), "description"),
                ConceptPayloadFields.requiredBoolean(body.get("active"), "active"),
                ConceptExpressionPayload.parseAll(body.get("expressions")));
    }

    ConceptCreateInput toInput() {
        return new ConceptCreateInput(
                key, displayName, description, active, ConceptExpressionPayload.toInputs(expressions));
    }
}
