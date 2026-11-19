package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.application.StandaloneArticlePage;
import vn.io.ajarline.content.contract.PublicContentSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one page of standalone public Articles. There is no total
 * count; {@code hasMore} tells whether a next page exists.
 */
@Schema(requiredProperties = {"page", "size", "hasMore", "items"})
public record ArticleCollectionResponse(
        int page,
        int size,
        boolean hasMore,
        List<ArticleSummary> items
) {

    public ArticleCollectionResponse {
        items = List.copyOf(items);
    }

    static ArticleCollectionResponse from(ArticleCollectionQuery query, StandaloneArticlePage result) {
        return new ArticleCollectionResponse(
                query.page(),
                query.size(),
                result.hasMore(),
                result.entries().stream().map(ArticleSummary::from).toList());
    }

    @Schema(requiredProperties = {
            "id", "type", "slug", "title", "summary",
            "topics", "publishedAt", "updatedAt"
    })
    public record ArticleSummary(
            UUID id,
            @Schema(allowableValues = "article") String type,
            String slug,
            String title,
            String summary,
            List<PublicArticleResponse.TopicReference> topics,
            Instant publishedAt,
            Instant updatedAt
    ) {

        public ArticleSummary {
            topics = List.copyOf(topics);
        }

        static ArticleSummary from(StandaloneArticlePage.Entry entry) {
            PublicContentSummary article = entry.content();
            return new ArticleSummary(
                    article.id(),
                    "article",
                    article.slug(),
                    article.title(),
                    article.summary(),
                    entry.topics().stream().map(PublicArticleResponse.TopicReference::from).toList(),
                    article.publishedAt(),
                    article.updatedAt());
        }
    }
}
