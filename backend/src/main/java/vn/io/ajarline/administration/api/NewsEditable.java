package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.NewsContentInput;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = "Complete editable News payload. Every field must be present.",
        requiredProperties = {"title", "slug", "summary", "bodyMarkdown", "source", "topicIds"}
)
public record NewsEditable(
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        NewsSourceEditable source,
        List<UUID> topicIds
) {

    private static final Set<String> FIELD_NAMES = Set.of(
            "title", "slug", "summary", "bodyMarkdown", "source", "topicIds");

    public NewsEditable {
        topicIds = List.copyOf(topicIds);
    }

    static NewsEditable parse(JsonNode body) {
        LearningPayloadFields.requireExactFields(body, FIELD_NAMES, "News");
        return new NewsEditable(
                LearningPayloadFields.requiredText(body.get("title"), "title"),
                LearningPayloadFields.requiredText(body.get("slug"), "slug"),
                LearningPayloadFields.nullableText(body.get("summary"), "summary"),
                LearningPayloadFields.nullableText(body.get("bodyMarkdown"), "bodyMarkdown"),
                NewsSourceEditable.parse(body.get("source")),
                LearningPayloadFields.uuidList(body.get("topicIds"), "topicIds"));
    }

    NewsContentInput toContentInput() {
        return new NewsContentInput(
                new ContentRecordInput(title, slug, summary, topicIds),
                bodyMarkdown,
                source.toSource());
    }
}
