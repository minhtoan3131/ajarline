package vn.io.ajarline.administration.api;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads the field kinds of Series and Module request bodies.
 * It checks the JSON shape only. Business rules belong to the capability that owns them.
 */
final class LearningPayloadFields {

    private LearningPayloadFields() {
    }

    static void requireExactFields(JsonNode body, Set<String> expectedFields, String payloadName) {
        if (body == null || !body.isObject()) {
            throw AdminApiException.invalidRequest("Request body must be a JSON object.");
        }
        if (!Set.copyOf(body.propertyNames()).equals(expectedFields)) {
            throw AdminApiException.invalidRequest(
                    "Request body must contain exactly the " + payloadName + " editable fields: "
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

    static int requiredInt(JsonNode value, String field) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw AdminApiException.invalidRequest(field + " must be a JSON integer.");
        }
        return value.asInt();
    }

    static UUID uuid(JsonNode value, String field) {
        if (value == null || !value.isString() || value.asString().length() != 36) {
            throw AdminApiException.invalidUuid(field);
        }
        try {
            return UUID.fromString(value.asString());
        } catch (IllegalArgumentException exception) {
            throw AdminApiException.invalidUuid(field);
        }
    }

    static List<UUID> uuidList(JsonNode value, String field) {
        if (value == null || !value.isArray()) {
            throw AdminApiException.invalidRequest(field + " must be a JSON array.");
        }
        List<UUID> result = new ArrayList<>();
        for (JsonNode item : value) {
            result.add(uuid(item, field));
        }
        return result;
    }
}
