package vn.io.ajarline.administration.api;

import tools.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Reads the field kinds used by Concept Admin request bodies.
 *
 * <p>Every method checks the JSON shape only. Business limits such as length,
 * key format and the number of expressions belong to the retrieval capability.</p>
 */
final class ConceptPayloadFields {

    private ConceptPayloadFields() {
    }

    static void requireExactFields(JsonNode value, Set<String> expectedFields, String subject) {
        if (value == null || !value.isObject()) {
            throw AdminApiException.invalidRequest(subject + " must be a JSON object.");
        }
        if (!Set.copyOf(value.propertyNames()).equals(expectedFields)) {
            throw AdminApiException.invalidRequest(
                    subject + " must contain exactly these fields: "
                            + String.join(", ", expectedFields.stream().sorted().toList()) + ".");
        }
    }

    static String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString()) {
            throw AdminApiException.invalidRequest(field + " must be a JSON string.");
        }
        return value.asString();
    }

    static String nullableText(JsonNode value, String field) {
        if (value == null) {
            throw AdminApiException.invalidRequest(field + " is required.");
        }
        if (value.isNull()) {
            return null;
        }
        return requiredText(value, field);
    }

    static boolean requiredBoolean(JsonNode value, String field) {
        if (value == null || !value.isBoolean()) {
            throw AdminApiException.invalidRequest(field + " must be a JSON boolean.");
        }
        return value.asBoolean();
    }

    static JsonNode requiredArray(JsonNode value, String field) {
        if (value == null || !value.isArray()) {
            throw AdminApiException.invalidRequest(field + " must be a JSON array.");
        }
        return value;
    }
}
