package vn.io.ajarline.publication.contract;

import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Describes canonical publication state after a publication operation.
 */
public record PublicationResult(
        UUID id,
        ContentType type,
        PublicationStatus status,
        long revision,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant updatedAt
) {
    public static PublicationResult from(ArticleView article) {
        return new PublicationResult(
                article.id(),
                article.type(),
                article.status(),
                article.revision(),
                article.publishedAt(),
                article.unpublishedAt(),
                article.updatedAt()
        );
    }
}
