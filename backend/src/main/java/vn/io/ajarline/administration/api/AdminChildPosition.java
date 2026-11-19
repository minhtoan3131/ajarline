package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.learning.contract.ChildPosition;

import java.util.UUID;

/**
 * Where one child sits after a reorder, with its new revision.
 */
@Schema(requiredProperties = {"id", "position", "revision"})
public record AdminChildPosition(UUID id, int position, long revision) {

    static AdminChildPosition from(ChildPosition child) {
        return new AdminChildPosition(child.id(), child.position(), child.revision());
    }
}
