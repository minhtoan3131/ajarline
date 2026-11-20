package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.application.PublicNewsPage;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.platform.web.PageQuery;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one page of public News. There is no total count; {@code hasMore} tells whether
 * a next page exists.
 */
@Schema(requiredProperties = {"page", "size", "hasMore", "items"})
public record NewsCollectionResponse(
        int page,
        int size,
        boolean hasMore,
        List<NewsSummary> items
) {

    public NewsCollectionResponse {
        items = List.copyOf(items);
    }

    static NewsCollectionResponse from(PageQuery query, PublicNewsPage result) {
        return new NewsCollectionResponse(
                query.page(),
                query.size(),
                result.hasMore(),
                result.entries().stream().map(NewsSummary::from).toList());
    }

    @Schema(requiredProperties = {
            "id", "type", "slug", "title", "summary",
            "topics", "publishedAt", "updatedAt"
    })
    public record NewsSummary(
            UUID id,
            @Schema(allowableValues = "news") String type,
            String slug,
            String title,
            String summary,
            List<PublicArticleResponse.TopicReference> topics,
            Instant publishedAt,
            Instant updatedAt
    ) {

        public NewsSummary {
            topics = List.copyOf(topics);
        }

        static NewsSummary from(PublicNewsPage.Entry entry) {
            PublicContentSummary news = entry.content();
            return new NewsSummary(
                    news.id(),
                    "news",
                    news.slug(),
                    news.title(),
                    news.summary(),
                    entry.topics().stream().map(PublicArticleResponse.TopicReference::from).toList(),
                    news.publishedAt(),
                    news.updatedAt());
        }
    }
}
