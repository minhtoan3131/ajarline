package vn.io.ajarline.content.contract;

import java.util.Optional;
import java.util.UUID;

public interface NewsReader {

    /**
     * Returns a News item whatever its status.
     *
     * @throws ContentOperationException with {@code CONTENT_NOT_FOUND}
     *         when the id is unknown or is not News
     */
    NewsView get(UUID id);

    /**
     * Returns the News item with the slug when it is published, together with the Topics
     * shown with it. A missing, draft or unpublished item gives the same empty result.
     *
     * @throws IllegalStateException when the item is published but its data is broken
     */
    Optional<PublicNewsView> findPublishedBySlug(String slug);
}
