package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.io.ajarline.content.contract.ArticleView;

/**
 * Represents one publicly eligible Article returned by the public API.
 */
@Schema(requiredProperties = {
        "id", "type", "slug", "title", "summary", "bodyMarkdown",
        "topics", "series", "module", "previousArticle", "nextArticle",
        "publishedAt", "updatedAt"
})
public record PublicArticleResponse(
        UUID id,
        @Schema(allowableValues = "article") String type,
        String slug,
        String title,
        String summary,
        String bodyMarkdown,
        List<TopicReference> topics,
        @Schema(nullable = true) SeriesReference series,
        @Schema(nullable = true) ModuleReference module,
        @Schema(nullable = true) ArticleSequenceReference previousArticle,
        @Schema(nullable = true) ArticleSequenceReference nextArticle,
        Instant publishedAt,
        Instant updatedAt
) {

    public PublicArticleResponse {
        topics = List.copyOf(topics);
    }

    static PublicArticleResponse from(ArticleView article) {
        return new PublicArticleResponse(
                article.id(),
                "article",
                article.slug(),
                article.title(),
                article.summary(),
                article.bodyMarkdown(),
                List.of(),
                null,
                null,
                null,
                null,
                article.publishedAt(),
                article.updatedAt());
    }

    @Schema(requiredProperties = {"id", "slug", "name"})
    public record TopicReference(
            UUID id,
            String slug,
            String name
    ) {
    }

    @Schema(requiredProperties = {"id", "slug", "title"})
    public record SeriesReference(
            UUID id,
            String slug,
            String title
    ) {
    }

    @Schema(requiredProperties = {"id", "slug", "title", "position"})
    public record ModuleReference(
            UUID id,
            String slug,
            String title,
            int position
    ) {
    }

    @Schema(requiredProperties = {"id", "slug", "title", "position"})
    public record ArticleSequenceReference(
            UUID id,
            String slug,
            String title,
            int position
    ) {
    }
}
