package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface NewsReader {

    /**
     * Returns a News item whatever its status.
     *
     * @throws ContentOperationException with {@code CONTENT_NOT_FOUND}
     *         when the id is unknown or is not News
     */
    NewsView get(UUID id);
}
