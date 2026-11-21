package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;

import java.util.List;
import java.util.Set;

/**
 * Represents the complete editable payload of a Concept at the Admin HTTP boundary.
 * It has no key because a Concept key never changes.
 */
@Schema(
        description = "Complete editable Concept payload. The key cannot be sent. The expressions replace the whole set.",
        requiredProperties = {"displayName", "description", "active", "expressions"}
)
public record ConceptEditable(
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionPayload> expressions
) {

    private static final Set<String> FIELD_NAMES =
            Set.of("displayName", "description", "active", "expressions");

    static ConceptEditable parse(JsonNode body) {
        ConceptPayloadFields.requireExactFields(body, FIELD_NAMES, "Request body");
        return new ConceptEditable(
                ConceptPayloadFields.requiredText(body.get("displayName"), "displayName"),
                ConceptPayloadFields.nullableText(body.get("description"), "description"),
                ConceptPayloadFields.requiredBoolean(body.get("active"), "active"),
                ConceptExpressionPayload.parseAll(body.get("expressions")));
    }

    ConceptUpdateInput toInput() {
        return new ConceptUpdateInput(
                displayName, description, active, ConceptExpressionPayload.toInputs(expressions));
    }
}
