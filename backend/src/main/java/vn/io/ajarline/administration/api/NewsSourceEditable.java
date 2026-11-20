package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.NewsSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Schema(
        description = "The outside source of a News item. All three keys must be present.",
        requiredProperties = {"name", "url", "publishedAt"}
)
public record NewsSourceEditable(
        String name,
        String url,
        @Schema(nullable = true) Instant publishedAt
) {

    private static final Set<String> FIELD_NAMES = Set.of("name", "url", "publishedAt");

    static NewsSourceEditable parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw AdminApiException.invalidRequest("source must be a JSON object.");
        }
        if (!Set.copyOf(node.propertyNames()).equals(FIELD_NAMES)) {
            throw AdminApiException.invalidRequest(
                    "source must contain exactly the keys: name, publishedAt, url.");
        }
        return new NewsSourceEditable(
                LearningPayloadFields.nullableText(node.get("name"), "source.name"),
                LearningPayloadFields.nullableText(node.get("url"), "source.url"),
                publishedAt(node.get("publishedAt")));
    }

    static NewsSourceEditable from(NewsSource source) {
        return new NewsSourceEditable(source.name(), source.url(), source.publishedAt());
    }

    NewsSource toSource() {
        return new NewsSource(name, url, publishedAt);
    }

    /**
     * Reads an ISO-8601 date-time with an offset, or null. A date alone, or a time without an
     * offset, is a wrong shape: the server would have to guess the moment.
     */
    private static Instant publishedAt(JsonNode value) {
        if (value == null) {
            throw AdminApiException.invalidRequest("source.publishedAt is required.");
        }
        if (value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw AdminApiException.invalidRequest("source.publishedAt must be a JSON string or null.");
        }
        try {
            return OffsetDateTime.parse(value.asString()).toInstant();
        } catch (DateTimeParseException exception) {
            throw AdminApiException.invalidRequest(
                    "source.publishedAt must be an ISO-8601 date-time with an offset.");
        }
    }
}
