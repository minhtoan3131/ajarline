package vn.io.ajarline.learning.contract;

import vn.io.ajarline.content.contract.PublicationStatus;

import java.util.UUID;

/**
 * One child of a Series or Module. The status is the child's own status, not whether readers see it.
 */
public record LearningChild(
        UUID id,
        String title,
        String slug,
        PublicationStatus status,
        int position
) {
}
