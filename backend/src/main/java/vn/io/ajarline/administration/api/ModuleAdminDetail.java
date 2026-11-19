package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.ModuleView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Module data of the Admin API, with its Articles in order. The Article list is read-only.
 */
@Schema(requiredProperties = {
        "id", "type", "seriesId", "position", "title", "slug", "summary",
        "overviewMarkdown", "topicIds", "status", "revision", "publishedAt",
        "unpublishedAt", "createdAt", "updatedAt", "articles"
})
public record ModuleAdminDetail(
        UUID id,
        String type,
        UUID seriesId,
        int position,
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
        List<AdminChildItem> articles
) {
    static ModuleAdminDetail from(ModuleView view) {
        ContentRecordView record = view.record();
        return new ModuleAdminDetail(
                record.id(),
                "module",
                view.seriesId(),
                view.position(),
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
                view.articles().stream().map(AdminChildItem::from).toList()
        );
    }
}
