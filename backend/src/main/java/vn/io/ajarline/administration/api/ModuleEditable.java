package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.learning.contract.ModuleContentInput;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = "Complete editable Module payload. seriesId and position are always required.",
        requiredProperties = {
                "seriesId", "position", "title", "slug", "summary", "overviewMarkdown", "topicIds"
        }
)
public record ModuleEditable(
        UUID seriesId,
        int position,
        String title,
        String slug,
        String summary,
        String overviewMarkdown,
        List<UUID> topicIds
) {

    private static final Set<String> FIELD_NAMES = Set.of(
            "seriesId", "position", "title", "slug", "summary", "overviewMarkdown", "topicIds");

    public ModuleEditable {
        topicIds = List.copyOf(topicIds);
    }

    static ModuleEditable parse(JsonNode body) {
        LearningPayloadFields.requireExactFields(body, FIELD_NAMES, "Module");
        return new ModuleEditable(
                LearningPayloadFields.uuid(body.get("seriesId"), "seriesId"),
                LearningPayloadFields.requiredInt(body.get("position"), "position"),
                LearningPayloadFields.requiredText(body.get("title"), "title"),
                LearningPayloadFields.requiredText(body.get("slug"), "slug"),
                LearningPayloadFields.nullableText(body.get("summary"), "summary"),
                LearningPayloadFields.nullableText(body.get("overviewMarkdown"), "overviewMarkdown"),
                LearningPayloadFields.uuidList(body.get("topicIds"), "topicIds"));
    }

    ModuleContentInput toContentInput() {
        return new ModuleContentInput(
                new ContentRecordInput(title, slug, summary, topicIds),
                overviewMarkdown,
                seriesId,
                position);
    }
}
