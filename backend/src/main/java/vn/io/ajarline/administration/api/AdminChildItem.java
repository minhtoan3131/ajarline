package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.LearningChild;

import java.util.UUID;

/**
 * One child in the ordered child list of a Series or Module.
 * The status is the child's own status, not whether readers see it.
 */
@Schema(requiredProperties = {"id", "title", "slug", "status", "position"})
public record AdminChildItem(
        UUID id,
        String title,
        String slug,
        PublicationStatus status,
        int position
) {
    static AdminChildItem from(LearningChild child) {
        return new AdminChildItem(
                child.id(), child.title(), child.slug(), child.status(), child.position());
    }
}
