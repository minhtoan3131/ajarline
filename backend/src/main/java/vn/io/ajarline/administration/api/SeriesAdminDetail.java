package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.SeriesView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Series data of the Admin API, with its Modules in order. The Module list is read-only.
 */
@Schema(requiredProperties = {
        "id", "type", "title", "slug", "summary", "overviewMarkdown", "topicIds",
        "status", "revision", "publishedAt", "unpublishedAt", "createdAt", "updatedAt",
        "modules"
})
public record SeriesAdminDetail(
        UUID id,
        String type,
        String title,
        String slug,
        String summary,
        String overviewMarkdown,
        List<UUID> topicIds,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<AdminChildItem> modules
) {
    static SeriesAdminDetail from(SeriesView view) {
        ContentRecordView record = view.record();
        return new SeriesAdminDetail(
                record.id(),
                "series",
                record.title(),
                record.slug(),
                record.summary(),
                view.overviewMarkdown(),
                view.topicIds(),
                record.status(),
                record.revision(),
                record.publishedAt(),
                record.unpublishedAt(),
                record.createdAt(),
                record.updatedAt(),
                view.modules().stream().map(AdminChildItem::from).toList()
        );
    }
}
