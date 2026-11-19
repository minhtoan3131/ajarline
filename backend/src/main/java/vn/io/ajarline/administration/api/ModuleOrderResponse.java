package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.learning.contract.ChildOrderView;

import java.util.List;
import java.util.UUID;

/**
 * The Modules of a Series after a reorder. {@code revision} is the revision of the Series.
 */
@Schema(requiredProperties = {"seriesId", "revision", "modules"})
public record ModuleOrderResponse(UUID seriesId, long revision, List<AdminChildPosition> modules) {

    static ModuleOrderResponse from(ChildOrderView view) {
        return new ModuleOrderResponse(
                view.parentId(),
                view.parentRevision(),
                view.children().stream().map(AdminChildPosition::from).toList());
    }
}
