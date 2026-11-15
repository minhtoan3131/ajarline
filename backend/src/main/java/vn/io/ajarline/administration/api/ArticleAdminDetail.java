package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents the Article data returned by the Admin API.
 */
@Schema(requiredProperties = {
        "id", "type", "title", "slug", "summary", "bodyMarkdown",
        "moduleId", "position", "topicIds", "status", "revision",
        "publishedAt", "unpublishedAt", "createdAt", "updatedAt"
})
public record ArticleAdminDetail(
        UUID id,
        String type,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        UUID moduleId,
        Integer position,
        List<UUID> topicIds,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant createdAt,
        Instant updatedAt
) {
    static ArticleAdminDetail from(ArticleView view) {
        // TODO: Compose moduleId and position from the learning capability.
        return new ArticleAdminDetail(
                view.id(),
                "article",
                view.title(),
                view.slug(),
                view.summary(),
                view.bodyMarkdown(),
                null,
                null,
                view.topicIds(),
                view.status(),
                view.revision(),
                view.publishedAt(),
                view.unpublishedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
