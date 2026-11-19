package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.learning.application.ModuleEntry;
import vn.io.ajarline.learning.application.PublicSeriesView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one public Series with its public Modules and their public Articles.
 */
@Schema(requiredProperties = {
        "id", "type", "slug", "title", "summary", "overviewMarkdown",
        "topics", "modules", "publishedAt", "updatedAt"
})
public record PublicSeriesResponse(
        UUID id,
        @Schema(allowableValues = "series") String type,
        String slug,
        String title,
        String summary,
        String overviewMarkdown,
        List<LearningTopicReference> topics,
        List<ModuleListItem> modules,
        Instant publishedAt,
        Instant updatedAt
) {

    public PublicSeriesResponse {
        topics = List.copyOf(topics);
        modules = List.copyOf(modules);
    }

    static PublicSeriesResponse from(PublicSeriesView view) {
        PublicContentSummary series = view.series();
        return new PublicSeriesResponse(
                series.id(),
                "series",
                series.slug(),
                series.title(),
                series.summary(),
                view.overviewMarkdown(),
                view.topics().stream().map(LearningTopicReference::from).toList(),
                view.modules().stream().map(ModuleListItem::from).toList(),
                series.publishedAt(),
                series.updatedAt());
    }

    @Schema(requiredProperties = {"id", "slug", "title", "summary", "position", "articles"})
    public record ModuleListItem(
            UUID id,
            String slug,
            String title,
            String summary,
            int position,
            List<ArticleListItem> articles
    ) {

        public ModuleListItem {
            articles = List.copyOf(articles);
        }

        static ModuleListItem from(ModuleEntry entry) {
            PublicContentSummary module = entry.module();
            return new ModuleListItem(
                    module.id(),
                    module.slug(),
                    module.title(),
                    module.summary(),
                    entry.position(),
                    entry.articles().stream().map(ArticleListItem::from).toList());
        }
    }
}
