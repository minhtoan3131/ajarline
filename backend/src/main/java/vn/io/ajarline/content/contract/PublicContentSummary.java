package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Holds the public fields of one content item that is public now.
 *
 * <p>It has no publication state, revision or creation time, so a caller
 * cannot show what the public must not see. The context has only the parents
 * that readers can see.</p>
 */
public record PublicContentSummary(
        UUID id,
        ContentType type,
        String slug,
        String title,
        String summary,
        Instant publishedAt,
        Instant updatedAt,
        PublicContentContext context
) {

    public PublicContentSummary {
        Objects.requireNonNull(context, "context");
    }

    /**
     * Returns the same item with another context.
     */
    public PublicContentSummary withContext(PublicContentContext context) {
        return new PublicContentSummary(
                id, type, slug, title, summary, publishedAt, updatedAt, context);
    }
}
