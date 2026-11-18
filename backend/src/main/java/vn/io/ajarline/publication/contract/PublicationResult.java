package vn.io.ajarline.publication.contract;

import vn.io.ajarline.content.contract.ContentRecordView;
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
    public static PublicationResult from(ContentRecordView record) {
        return new PublicationResult(
                record.id(),
                record.type(),
                record.status(),
                record.revision(),
                record.publishedAt(),
                record.unpublishedAt(),
                record.updatedAt()
        );
    }
}
