package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface ArticleRemover {

    /**
     * Deletes an Article that was never published, when {@code expectedRevision} is current.
     * An Article in a Module leaves it, the later Articles move up, and the revisions of the
     * Module and of the moved Articles rise by one.
     *
     * @throws ContentOperationException with {@code CONTENT_NOT_FOUND},
     *         {@code REVISION_CONFLICT} or {@code CONTENT_ALREADY_EVER_PUBLISHED}
     */
    void delete(UUID id, long expectedRevision);
}
