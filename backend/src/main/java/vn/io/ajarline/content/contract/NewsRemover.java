package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface NewsRemover {

    /**
     * Deletes a News item that was never published, when {@code expectedRevision} is current.
     *
     * @throws ContentOperationException with {@code CONTENT_NOT_FOUND},
     *         {@code REVISION_CONFLICT} or {@code CONTENT_ALREADY_EVER_PUBLISHED}
     */
    void delete(UUID id, long expectedRevision);
}
