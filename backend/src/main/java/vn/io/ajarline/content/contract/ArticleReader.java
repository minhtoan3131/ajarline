package vn.io.ajarline.content.contract;

import java.util.Optional;
import java.util.UUID;

public interface ArticleReader {
    ArticleView get(UUID id);

    Optional<ArticleView> findPublishedBySlug(String slug);
}
