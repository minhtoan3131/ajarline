package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.learning.contract.ChildOrderView;

import java.util.List;
import java.util.UUID;

/**
 * The Articles of a Module after a reorder. {@code revision} is the revision of the Module.
 */
@Schema(requiredProperties = {"moduleId", "revision", "articles"})
public record ArticleOrderResponse(UUID moduleId, long revision, List<AdminChildPosition> articles) {

    static ArticleOrderResponse from(ChildOrderView view) {
        return new ArticleOrderResponse(
                view.parentId(),
                view.parentRevision(),
                view.children().stream().map(AdminChildPosition::from).toList());
    }
}
