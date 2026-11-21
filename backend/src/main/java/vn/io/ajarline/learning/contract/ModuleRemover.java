package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface ModuleRemover {

    /**
     * Deletes a Module that was never published and has no Article, when
     * {@code expectedRevision} is current. The later Modules of its Series move up. The
     * revisions of the Series and of the moved Modules rise by one.
     *
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND}, {@code REVISION_CONFLICT} or
     *         {@code CONTENT_ALREADY_EVER_PUBLISHED}
     * @throws LearningOperationException with {@code CONTENT_IN_USE} when the Module has an Article
     */
    void delete(UUID id, long expectedRevision);
}
