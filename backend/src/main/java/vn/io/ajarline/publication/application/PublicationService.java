package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.*;

import java.time.Clock;
import java.util.UUID;

@Service
public class PublicationService implements ContentPublisher, ContentUnpublisher {
    private final ContentPublicationAccess access;
    private final ArticleReader articles;
    private final ArticlePublicationPolicy policy;
    private final Clock clock;

    public PublicationService(
            ContentPublicationAccess access,
            ArticleReader articles,
            ArticlePublicationPolicy policy,
            Clock clock
    ) {
        this.access = access;
        this.articles = articles;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicationResult publish(UUID id, long expectedRevision) {
        ContentRecordView current = access.lock(id, expectedRevision);
        if (current.status() == PublicationStatus.PUBLISHED) {
            return PublicationResult.from(current);
        }

        // NOTE: the publish rules need the Article body, which the common record does not carry.
        policy.validateForPublish(articles.get(id));
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
