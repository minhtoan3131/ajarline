package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentEligibility;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.*;

import java.time.Clock;
import java.util.UUID;

@Service
public class PublicationService implements ContentPublisher, ContentUnpublisher {
    private final ContentPublicationAccess access;
    private final ContentEligibility eligibility;
    private final Clock clock;

    public PublicationService(
            ContentPublicationAccess access,
            ContentEligibility eligibility,
            Clock clock
    ) {
        this.access = access;
        this.eligibility = eligibility;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicationResult publish(UUID id, long expectedRevision) {
        ContentRecordView current = access.lock(id, expectedRevision);
        if (current.status() == PublicationStatus.PUBLISHED) {
            return PublicationResult.from(current);
        }

        eligibility.requirePublishable(id);
        ContentRecordView published = access.markPublished(id, expectedRevision, clock.instant());
        return PublicationResult.from(published);
    }

    @Override
    @Transactional
    public PublicationResult unpublish(UUID id, long expectedRevision) {
        ContentRecordView current = access.lock(id, expectedRevision);
        return switch (current.status()) {
            case DRAFT -> throw new PublicationOperationException(
                    PublicationFailureCode.INVALID_PUBLICATION_STATE,
                    "A Draft Article cannot be unpublished."
            );
            case UNPUBLISHED -> PublicationResult.from(current);
            case PUBLISHED -> PublicationResult.from(
                    access.markUnpublished(id, expectedRevision, clock.instant())
            );
        };
    }
}
