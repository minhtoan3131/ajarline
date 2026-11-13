package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides Article operations used by the publication workflow.
 *
 * <p>Publication decides when an Article can be published or unpublished.
 * Content owns the Article data and applies the actual changes.</p>
 */
public interface ArticlePublicationAccess {
    /**
     * Locks and returns the Article at the expected revision.
     *
     * @throws ContentOperationException when the Article does not exist or the
     *         revision is stale
     */
    ArticleView lockForPublication(UUID id, long expectedRevision);

    /**
     * Applies one effective transition to PUBLISHED.
     */
    ArticleView markPublished(UUID id, long expectedRevision, Instant changedAt);

    /**
     * Applies one effective transition from PUBLISHED to UNPUBLISHED.
     */
    ArticleView markUnpublished(UUID id, long expectedRevision, Instant changedAt);
}
