package vn.io.ajarline.administration.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ArticleContentInput;

/**
 * Represents the complete editable Article payload at the Admin HTTP boundary.
 */
@Schema(
        description = "Complete editable Article payload. moduleId and position must both be null or both be present.",
        requiredProperties = {
                "title", "slug", "summary", "bodyMarkdown", "moduleId", "position", "topicIds"
        }
)
public record ArticleEditable(
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        UUID moduleId,
        Integer position,
        List<UUID> topicIds
) {

    private static final Set<String> FIELD_NAMES = Set.of(
            "title", "slug", "summary", "bodyMarkdown",
            "moduleId", "position", "topicIds");

    public ArticleEditable {
        topicIds = List.copyOf(topicIds);
    }

    static ArticleEditable parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw AdminApiException.invalidRequest(
                    "Request body must be a JSON object.");
        }

        Set<String> actualFields = Set.copyOf(body.propertyNames());
        if (!actualFields.equals(FIELD_NAMES)) {
            throw AdminApiException.invalidRequest(
                    "Request body must contain exactly the Article editable fields.");
        }

        List<UUID> topicIds = topicIds(body.get("topicIds"));
        if (new HashSet<>(topicIds).size() != topicIds.size()) {
            throw AdminApiException.unprocessable(
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    "topicIds must not contain duplicate values.");
        }

        ArticleEditable editable = new ArticleEditable(
                requiredText(body.get("title"), "title"),
                requiredText(body.get("slug"), "slug"),
                nullableText(body.get("summary"), "summary"),
                nullableText(body.get("bodyMarkdown"), "bodyMarkdown"),
                nullableUuid(body.get("moduleId"), "moduleId"),
                nullablePosition(body.get("position")),
                topicIds);
        editable.validateRelationships();
        return editable;
    }

    ArticleContentInput toContentInput() {
        return new ArticleContentInput(title, slug, summary, bodyMarkdown);
    }

    private void validateRelationships() {
        if ((moduleId == null) != (position == null)) {
            throw AdminApiException.unprocessable(
                    "INVALID_POSITION",
                    "Article position is invalid",
                    "moduleId and position must either both be present or both be null.");
        }
    }

    private static String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString()) {
            throw AdminApiException.invalidRequest(
                    field + " must be a JSON string.");
        }
        return value.asString();
    }

    private static String nullableText(JsonNode value, String field) {
        if (value == null) {
            throw AdminApiException.invalidRequest(field + " is required.");
        }
        if (value.isNull()) {
            return null;
        }
        return requiredText(value, field);
    }

    private static UUID nullableUuid(JsonNode value, String field) {
        if (value == null) {
            throw AdminApiException.invalidRequest(field + " is required.");
        }
        if (value.isNull()) {
            return null;
        }
        return uuid(value, field);
    }

    private static Integer nullablePosition(JsonNode value) {
        if (value == null) {
            throw AdminApiException.invalidRequest("position is required.");
        }
        if (value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw AdminApiException.invalidRequest(
                    "position must be an integer or null.");
        }
        int position = value.asInt();
        if (position < 1) {
            throw AdminApiException.unprocessable(
                    "INVALID_POSITION",
                    "Article position is invalid",
                    "position must be at least 1.");
        }
        return position;
    }

    private static List<UUID> topicIds(JsonNode value) {
        if (value == null || !value.isArray()) {
            throw AdminApiException.invalidRequest(
                    "topicIds must be a JSON array.");
        }
        List<UUID> result = new ArrayList<>();
        for (JsonNode item : value) {
            result.add(uuid(item, "topicIds"));
        }
        return result;
    }

    private static UUID uuid(JsonNode value, String field) {
        if (value == null || !value.isString()
                || value.asString().length() != 36) {
            throw AdminApiException.invalidUuid(field);
        }
        try {
            return UUID.fromString(value.asString());
        } catch (IllegalArgumentException exception) {
            throw AdminApiException.invalidUuid(field);
        }
    }
}
