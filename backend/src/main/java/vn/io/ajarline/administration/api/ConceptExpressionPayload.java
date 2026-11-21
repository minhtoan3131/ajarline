package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents one expression of a Concept at the Admin HTTP boundary.
 */
@Schema(
        description = "One expression of a Concept. The language hint may be null.",
        requiredProperties = {"expression", "languageHint", "autoExpand"}
)
public record ConceptExpressionPayload(
        String expression,
        String languageHint,
        boolean autoExpand
) {

    private static final Set<String> FIELD_NAMES = Set.of("expression", "languageHint", "autoExpand");

    static ConceptExpressionPayload parse(JsonNode value) {
        ConceptPayloadFields.requireExactFields(value, FIELD_NAMES, "Each expression");
        return new ConceptExpressionPayload(
                ConceptPayloadFields.requiredText(value.get("expression"), "expression"),
                ConceptPayloadFields.nullableText(value.get("languageHint"), "languageHint"),
                ConceptPayloadFields.requiredBoolean(value.get("autoExpand"), "autoExpand"));
    }

    static List<ConceptExpressionPayload> parseAll(JsonNode value) {
        List<ConceptExpressionPayload> payloads = new ArrayList<>();
        for (JsonNode element : ConceptPayloadFields.requiredArray(value, "expressions")) {
            payloads.add(parse(element));
        }
        return payloads;
    }

    static List<ConceptExpressionInput> toInputs(List<ConceptExpressionPayload> payloads) {
        return payloads.stream().map(ConceptExpressionPayload::toInput).toList();
    }

    ConceptExpressionInput toInput() {
        return new ConceptExpressionInput(expression, languageHint, autoExpand);
    }
}
