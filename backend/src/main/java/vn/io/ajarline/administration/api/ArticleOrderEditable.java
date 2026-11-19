package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = "The ids of all Articles of the Module, in the new order. Each id once.",
        requiredProperties = {"articleIds"}
)
public record ArticleOrderEditable(List<UUID> articleIds) {

    private static final Set<String> FIELD_NAMES = Set.of("articleIds");

    public ArticleOrderEditable {
        articleIds = List.copyOf(articleIds);
    }

    static ArticleOrderEditable parse(JsonNode body) {
        LearningPayloadFields.requireExactFields(body, FIELD_NAMES, "Article order");
        return new ArticleOrderEditable(
                LearningPayloadFields.uuidList(body.get("articleIds"), "articleIds"));
    }
}
