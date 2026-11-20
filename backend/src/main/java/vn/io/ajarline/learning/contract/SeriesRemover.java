package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface SeriesRemover {

    /**
     * Deletes a Series that was never published and has no Module, when
     * {@code expectedRevision} is current.
     *
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND}, {@code REVISION_CONFLICT} or
     *         {@code CONTENT_ALREADY_EVER_PUBLISHED}
     * @throws LearningOperationException with {@code CONTENT_IN_USE} when the Series has a Module
     */
    void delete(UUID id, long expectedRevision);
}
