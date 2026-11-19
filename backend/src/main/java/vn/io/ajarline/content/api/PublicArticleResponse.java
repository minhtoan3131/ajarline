package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.PublicArticleContext;
import vn.io.ajarline.content.contract.PublicArticleRef;
import vn.io.ajarline.content.contract.PublicArticleView;
import vn.io.ajarline.content.contract.PublicModuleRef;
import vn.io.ajarline.content.contract.PublicSeriesRef;
import vn.io.ajarline.content.contract.TopicLabel;

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

    static PublicArticleResponse from(PublicArticleView view) {
        ArticleView article = view.article();
        PublicArticleContext learning = view.learning();
        return new PublicArticleResponse(
                article.id(),
                "article",
                article.slug(),
                article.title(),
                article.summary(),
                article.bodyMarkdown(),
                view.topics().stream().map(TopicReference::from).toList(),
                SeriesReference.from(learning.context().series()),
                ModuleReference.from(learning.context().module()),
                ArticleSequenceReference.from(learning.previous()),
                ArticleSequenceReference.from(learning.next()),
                article.publishedAt(),
                article.updatedAt());
    }

    @Schema(requiredProperties = {"id", "slug", "name"})
    public record TopicReference(
            UUID id,
            String slug,
            String name
    ) {
        static TopicReference from(TopicLabel label) {
            return new TopicReference(label.id(), label.slug(), label.name());
        }
    }

    @Schema(requiredProperties = {"id", "slug", "title"})
    public record SeriesReference(
            UUID id,
            String slug,
            String title
    ) {
        static SeriesReference from(PublicSeriesRef ref) {
            return ref == null ? null : new SeriesReference(ref.id(), ref.slug(), ref.title());
        }
    }

    @Schema(requiredProperties = {"id", "slug", "title", "position"})
    public record ModuleReference(
            UUID id,
            String slug,
            String title,
            int position
    ) {
        static ModuleReference from(PublicModuleRef ref) {
            return ref == null
                    ? null
                    : new ModuleReference(ref.id(), ref.slug(), ref.title(), ref.position());
        }
    }

    @Schema(requiredProperties = {"id", "slug", "title", "position"})
    public record ArticleSequenceReference(
            UUID id,
            String slug,
            String title,
            int position
    ) {
        static ArticleSequenceReference from(PublicArticleRef ref) {
            return ref == null
                    ? null
                    : new ArticleSequenceReference(ref.id(), ref.slug(), ref.title(), ref.position());
        }
    }
}
