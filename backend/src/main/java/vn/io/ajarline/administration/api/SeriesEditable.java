package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.learning.contract.SeriesContentInput;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = "Complete editable Series payload. Every field must be present.",
        requiredProperties = {"title", "slug", "summary", "overviewMarkdown", "topicIds"}
)
public record SeriesEditable(
        String title,
        String slug,
        String summary,
        String overviewMarkdown,
        List<UUID> topicIds
) {

    private static final Set<String> FIELD_NAMES = Set.of(
            "title", "slug", "summary", "overviewMarkdown", "topicIds");

    public SeriesEditable {
        topicIds = List.copyOf(topicIds);
    }

    static SeriesEditable parse(JsonNode body) {
        LearningPayloadFields.requireExactFields(body, FIELD_NAMES, "Series");
        return new SeriesEditable(
                LearningPayloadFields.requiredText(body.get("title"), "title"),
                LearningPayloadFields.requiredText(body.get("slug"), "slug"),
                LearningPayloadFields.nullableText(body.get("summary"), "summary"),
                LearningPayloadFields.nullableText(body.get("overviewMarkdown"), "overviewMarkdown"),
                LearningPayloadFields.uuidList(body.get("topicIds"), "topicIds"));
    }

    SeriesContentInput toContentInput() {
        return new SeriesContentInput(
                new ContentRecordInput(title, slug, summary, topicIds),
                overviewMarkdown);
    }
}
