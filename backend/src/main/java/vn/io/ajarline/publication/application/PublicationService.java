package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ArticlePublicationAccess;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.*;

import java.time.Clock;
import java.util.UUID;

@Service
public class PublicationService implements ContentPublisher, ContentUnpublisher {
    private final ArticlePublicationAccess articles;
    private final ArticlePublicationPolicy policy;
    private Clock clock;

    public PublicationService(ArticlePublicationAccess articles, ArticlePublicationPolicy policy, Clock clock) {
        this.articles = articles;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicationResult publish(UUID id, long expectedRevision) {
        ArticleView current = articles.lockForPublication(id, expectedRevision);
        if (current.status() == PublicationStatus.PUBLISHED) {
            return PublicationResult.from(current);
        }

        policy.validateForPublish(current);
        ArticleView published = articles.markPublished(id, expectedRevision, clock.instant());
        return PublicationResult.from(published);
    }

    @Override
    @Transactional
    public PublicationResult unpublish(UUID id, long expectedRevision) {
        ArticleView current = articles.lockForPublication(id, expectedRevision);
        return switch (current.status()) {
            case DRAFT -> throw new PublicationOperationException(
                    PublicationFailureCode.INVALID_PUBLICATION_STATE,
                    "A Draft Article cannot be unpublished."
            );
            case UNPUBLISHED -> PublicationResult.from(current);
            case PUBLISHED -> PublicationResult.from(
                    articles.markUnpublished(id, expectedRevision, clock.instant())
            );
        };
    }
}
