package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = "The ids of all Modules of the Series, in the new order. Each id once.",
        requiredProperties = {"moduleIds"}
)
public record ModuleOrderEditable(List<UUID> moduleIds) {

    private static final Set<String> FIELD_NAMES = Set.of("moduleIds");

    public ModuleOrderEditable {
        moduleIds = List.copyOf(moduleIds);
    }

    static ModuleOrderEditable parse(JsonNode body) {
        LearningPayloadFields.requireExactFields(body, FIELD_NAMES, "Module order");
        return new ModuleOrderEditable(
                LearningPayloadFields.uuidList(body.get("moduleIds"), "moduleIds"));
    }
}
