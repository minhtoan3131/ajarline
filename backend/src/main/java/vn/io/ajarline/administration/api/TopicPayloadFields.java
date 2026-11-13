package vn.io.ajarline.administration.api;

import tools.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Reads the field kinds used by Topic Admin request bodies.
 *
 * <p>Every method checks the JSON shape only. Business limits such as length
 * and slug format belong to the Topic capability.</p>
 */
final class TopicPayloadFields {

    private TopicPayloadFields() {
    }

    static void requireExactFields(JsonNode body, Set<String> expectedFields) {
        if (body == null || !body.isObject()) {
            throw AdminApiException.invalidRequest("Request body must be a JSON object.");
        }
        if (!Set.copyOf(body.propertyNames()).equals(expectedFields)) {
            throw AdminApiException.invalidRequest(
                    "Request body must contain exactly the Topic fields: "
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
}
