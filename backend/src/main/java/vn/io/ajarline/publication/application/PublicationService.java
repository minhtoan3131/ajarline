package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentEligibility;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.LearningEligibility;
import vn.io.ajarline.learning.contract.LearningStructureQuery;
import vn.io.ajarline.learning.contract.LearningVisibility;
import vn.io.ajarline.publication.contract.*;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Publishes and unpublishes content of any type.
 *
 * <p>The steps are the same for every type: lock, compare the revision, check, change the state.
 * The checks depend on the type. Unpublish never touches the children.</p>
 */
@Service
public class PublicationService implements ContentPublisher, ContentUnpublisher {
    private final ContentPublicationAccess access;
    private final ContentEligibility contentEligibility;
    private final LearningEligibility learningEligibility;
    private final LearningStructureQuery structure;
    private final LearningVisibility visibility;
    private final ContentRecordReader records;
    private final StructurePublicationPolicy structurePolicy;
    private final Clock clock;

    public PublicationService(
            ContentPublicationAccess access,
            ContentEligibility contentEligibility,
            LearningEligibility learningEligibility,
            LearningStructureQuery structure,
            LearningVisibility visibility,
            ContentRecordReader records,
            StructurePublicationPolicy structurePolicy,
            Clock clock
    ) {
        this.access = access;
        this.contentEligibility = contentEligibility;
        this.learningEligibility = learningEligibility;
        this.structure = structure;
        this.visibility = visibility;
        this.records = records;
        this.structurePolicy = structurePolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicationResult publish(UUID id, long expectedRevision) {
        ContentRecordView current = access.lock(id, expectedRevision);
        if (current.status() == PublicationStatus.PUBLISHED) {
            return PublicationResult.from(current);
        }

        requirePublishable(current);
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
                    "Draft content cannot be unpublished."
            );
            case UNPUBLISHED -> PublicationResult.from(current);
            case PUBLISHED -> PublicationResult.from(
                    access.markUnpublished(id, expectedRevision, clock.instant())
            );
        };
    }

    /**
     * Checks the fields first, then the children. An editor then fixes the fields of the
     * item itself before the question of its children.
     */
    private void requirePublishable(ContentRecordView current) {
        UUID id = current.id();
        contentEligibility.requirePublishable(id);
        switch (current.type()) {
            case ARTICLE -> {
            }
            case MODULE -> {
                learningEligibility.requirePublishableModule(id);
                structurePolicy.requireModulePublishable(articleStatusesOf(id));
            }
            case SERIES -> {
                learningEligibility.requirePublishableSeries(id);
                structurePolicy.requireSeriesPublishable(
                        visibility.publicModuleIds(structure.moduleIdsOf(id)));
            }
        }
    }

    private List<PublicationStatus> articleStatusesOf(UUID moduleId) {
        List<UUID> articleIds = structure.articleIdsOf(moduleId);
        return records.summariesOf(articleIds).values().stream()
                .map(ContentRecordSummary::status)
                .toList();
    }
}
