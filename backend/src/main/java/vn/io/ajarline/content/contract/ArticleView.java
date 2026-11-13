package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.UUID;

/**
 * Holds a snapshot of an Article's current state returned by the content service.
 *
 * <p>This is an immutable output object, not a database entity or an HTTP response.
 * It contains the result of creating, reading, or updating an Article.
 * Changes to the database do not change an existing ArticleView.</p>
 */
public record ArticleView(
        UUID id,
        ContentType type,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
