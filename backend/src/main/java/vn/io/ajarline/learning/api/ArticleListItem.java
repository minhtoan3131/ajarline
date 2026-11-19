package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.learning.application.ArticleEntry;

import java.util.UUID;

/**
 * Represents one public Article in the list of a Module. The position is its rank, from 1.
 */
@Schema(requiredProperties = {"id", "slug", "title", "summary", "position"})
public record ArticleListItem(
        UUID id,
        String slug,
        String title,
        String summary,
        int position
) {

    static ArticleListItem from(ArticleEntry entry) {
        PublicContentSummary article = entry.article();
        return new ArticleListItem(
                article.id(), article.slug(), article.title(), article.summary(), entry.position());
    }
}
