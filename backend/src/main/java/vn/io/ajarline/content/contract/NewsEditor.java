package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface NewsEditor {

    /**
     * Replaces the editable state when {@code expectedRevision} is current.
     * A published News item must keep its summary, body and source.
     *
     * @throws ContentOperationException with {@code NEWS_BODY_REQUIRED} or
     *         {@code NEWS_SOURCE_REQUIRED} when a published News item would lose them
     */
    NewsView replace(UUID id, long expectedRevision, NewsContentInput input);
}
