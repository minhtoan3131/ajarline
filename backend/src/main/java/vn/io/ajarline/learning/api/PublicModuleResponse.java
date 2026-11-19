package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicSeriesRef;
import vn.io.ajarline.learning.application.PublicModuleView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one public Module with its public Articles. The Series is null when readers
 * cannot see it.
 */
@Schema(requiredProperties = {
        "id", "type", "slug", "title", "summary", "overviewMarkdown",
        "topics", "series", "articles", "publishedAt", "updatedAt"
})
public record PublicModuleResponse(
        UUID id,
        @Schema(allowableValues = "module") String type,
        String slug,
        String title,
        String summary,
        String overviewMarkdown,
        List<LearningTopicReference> topics,
        @Schema(nullable = true) ParentSeries series,
        List<ArticleListItem> articles,
        Instant publishedAt,
        Instant updatedAt
) {

    public PublicModuleResponse {
        topics = List.copyOf(topics);
        articles = List.copyOf(articles);
    }

    static PublicModuleResponse from(PublicModuleView view) {
        PublicContentSummary module = view.module();
        return new PublicModuleResponse(
                module.id(),
                "module",
                module.slug(),
                module.title(),
                module.summary(),
                view.overviewMarkdown(),
                view.topics().stream().map(LearningTopicReference::from).toList(),
                ParentSeries.from(view.series()),
                view.articles().stream().map(ArticleListItem::from).toList(),
                module.publishedAt(),
                module.updatedAt());
    }

    @Schema(requiredProperties = {"id", "slug", "title"})
    public record ParentSeries(
            UUID id,
            String slug,
            String title
    ) {

        static ParentSeries from(PublicSeriesRef ref) {
            return ref == null ? null : new ParentSeries(ref.id(), ref.slug(), ref.title());
        }
    }
}
