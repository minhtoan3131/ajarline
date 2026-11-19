package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.learning.application.PublicSeriesPage;
import vn.io.ajarline.platform.web.PageQuery;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one page of public Series. There is no total count; {@code hasMore} tells
 * whether a next page exists.
 */
@Schema(requiredProperties = {"page", "size", "hasMore", "items"})
public record SeriesCollectionResponse(
        int page,
        int size,
        boolean hasMore,
        List<SeriesSummary> items
) {

    public SeriesCollectionResponse {
        items = List.copyOf(items);
    }

    static SeriesCollectionResponse from(PageQuery query, PublicSeriesPage result) {
        return new SeriesCollectionResponse(
                query.page(),
                query.size(),
                result.hasMore(),
                result.entries().stream().map(SeriesSummary::from).toList());
    }

    @Schema(requiredProperties = {
            "id", "type", "slug", "title", "summary", "topics", "publishedAt"
    })
    public record SeriesSummary(
            UUID id,
            @Schema(allowableValues = "series") String type,
            String slug,
            String title,
            String summary,
            List<LearningTopicReference> topics,
            Instant publishedAt,
            Instant updatedAt
    ) {

        public SeriesSummary {
            topics = List.copyOf(topics);
        }

        static SeriesSummary from(PublicSeriesPage.Entry entry) {
            PublicContentSummary series = entry.content();
            return new SeriesSummary(
                    series.id(),
                    "series",
                    series.slug(),
                    series.title(),
                    series.summary(),
                    entry.topics().stream().map(LearningTopicReference::from).toList(),
                    series.publishedAt(),
                    series.updatedAt());
        }
    }
}
