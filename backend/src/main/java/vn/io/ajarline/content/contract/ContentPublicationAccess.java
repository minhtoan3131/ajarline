package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides the content operations used by the publication workflow.
 *
 * <p>Publication decides when content can be published or unpublished.
 * Content owns the data and applies the actual changes, including the revision.
 * Every method must run inside the caller's transaction.</p>
 */
public interface ContentPublicationAccess {
    /**
     * Locks the content row and returns it at the expected revision.
     *
     * @throws ContentOperationException when the content does not exist or the
     *         revision is stale
     */
    ContentRecordView lock(UUID id, long expectedRevision);

    /**
     * Applies one effective transition to PUBLISHED.
     */
    ContentRecordView markPublished(UUID id, long expectedRevision, Instant changedAt);

    /**
     * Applies one effective transition from PUBLISHED to UNPUBLISHED.
     */
    ContentRecordView markUnpublished(UUID id, long expectedRevision, Instant changedAt);
}
