package vn.io.ajarline.content.contract;

import java.util.Optional;
import java.util.UUID;

public interface ArticleReader {
    ArticleView get(UUID id);

    /**
     * Returns the Article with the slug when it is published, together with
     * the Topics shown with it, ordered by name and then by id.
     */
    Optional<PublicArticleView> findPublishedBySlug(String slug);
}
