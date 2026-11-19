package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.NewsView;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * News data of the Admin API.
 */
@Schema(requiredProperties = {
        "id", "type", "title", "slug", "summary", "bodyMarkdown", "source", "topicIds",
        "status", "revision", "publishedAt", "unpublishedAt", "createdAt", "updatedAt"
})
public record NewsAdminDetail(
        UUID id,
        @Schema(allowableValues = "news") String type,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        NewsSourceEditable source,
        List<UUID> topicIds,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant createdAt,
        Instant updatedAt
) {
    static NewsAdminDetail from(NewsView view) {
        ContentRecordView record = view.record();
        return new NewsAdminDetail(
                record.id(),
                "news",
                record.title(),
                record.slug(),
                record.summary(),
                view.bodyMarkdown(),
                NewsSourceEditable.from(view.source()),
                view.topicIds(),
                record.status(),
                record.revision(),
                record.publishedAt(),
                record.unpublishedAt(),
                record.createdAt(),
                record.updatedAt());
    }
}
