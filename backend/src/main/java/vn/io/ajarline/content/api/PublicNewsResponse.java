package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.io.ajarline.content.contract.NewsSource;
import vn.io.ajarline.content.contract.PublicNewsView;

/**
 * Represents one public News item returned by the public API.
 */
@Schema(requiredProperties = {
        "id", "type", "slug", "title", "summary", "bodyMarkdown",
        "topics", "source", "publishedAt", "updatedAt"
})
public record PublicNewsResponse(
        UUID id,
        @Schema(allowableValues = "news") String type,
        String slug,
        String title,
        String summary,
        String bodyMarkdown,
        List<PublicArticleResponse.TopicReference> topics,
        NewsSourcePublic source,
        Instant publishedAt,
        Instant updatedAt
) {

    public PublicNewsResponse {
        topics = List.copyOf(topics);
    }

    static PublicNewsResponse from(PublicNewsView view) {
        return new PublicNewsResponse(
                view.id(),
                "news",
                view.slug(),
                view.title(),
                view.summary(),
                view.bodyMarkdown(),
                view.topics().stream().map(PublicArticleResponse.TopicReference::from).toList(),
                NewsSourcePublic.from(view.source()),
                view.publishedAt(),
                view.updatedAt());
    }

    /**
     * Represents the outside source. The date is null when the source gave none.
     */
    @Schema(requiredProperties = {"name", "url", "publishedAt"})
    public record NewsSourcePublic(
            String name,
            String url,
            @Schema(nullable = true) Instant publishedAt
    ) {
        static NewsSourcePublic from(NewsSource source) {
            return new NewsSourcePublic(source.name(), source.url(), source.publishedAt());
        }
    }
}
