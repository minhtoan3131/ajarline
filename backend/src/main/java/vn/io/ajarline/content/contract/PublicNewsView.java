package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Holds one public News item: only what readers may see.
 *
 * <p>It has no status, revision or Topic ids, so a caller cannot show them. A public News
 * item must be complete. If it is not, the data is broken and the constructor fails
 * instead of letting an empty value reach the reader.</p>
 *
 * @param source the outside source; only {@code source.publishedAt()} may be null
 * @param topics ordered by name and then by id
 * @param publishedAt when Ajarline first published it, not when the source did
 */
public record PublicNewsView(
        UUID id,
        String slug,
        String title,
        String summary,
        String bodyMarkdown,
        NewsSource source,
        List<TopicLabel> topics,
        Instant publishedAt,
        Instant updatedAt
) {

    public PublicNewsView {
        requireText(id, "summary", summary);
        requireText(id, "body", bodyMarkdown);
        requireText(id, "source name", source.name());
        requireText(id, "source URL", source.url());
        topics = List.copyOf(topics);
    }

    private static void requireText(UUID id, String part, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Public News " + id + " has no " + part + ".");
        }
    }
}
