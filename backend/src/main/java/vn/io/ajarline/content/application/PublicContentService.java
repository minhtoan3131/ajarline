package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tells other capabilities which content is public now.
 *
 * <p>An Article is public when it is published. This is the only place
 * outside the single-Article read that applies that rule.</p>
 */
@Service
public class PublicContentService implements PublicContentQuery {
    private static final PublicationStatus PUBLIC_STATUS = PublicationStatus.PUBLISHED;

    private final ContentItemJpaRepository contentRepository;

    public PublicContentService(ContentItemJpaRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findPublicIds(Collection<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(contentRepository.findIdsWithStatus(contentIds, PUBLIC_STATUS));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicContentPage findPublicPage(
            Collection<UUID> contentIds,
            Set<ContentType> types,
            int page,
            int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be at least 0 and size at least 1.");
        }
        if (contentIds.isEmpty() || types.isEmpty()) {
            return PublicContentPage.empty();
        }

        List<String> typeNames = types.stream().map(Enum::name).toList();
        long offset = (long) page * size;
        // One extra row tells whether a next page exists, without a count.
        List<ContentItemEntity> window = contentRepository.findWindowWithStatus(
                contentIds, PUBLIC_STATUS.name(), typeNames, size + 1, offset);

        List<PublicContentSummary> items = window.stream()
                .limit(size)
                .map(this::toSummary)
                .toList();
        return new PublicContentPage(items, window.size() > size);
    }

    private PublicContentSummary toSummary(ContentItemEntity content) {
        return new PublicContentSummary(
                content.getId(),
                content.getType(),
                content.getSlug(),
                content.getTitle(),
                content.getSummary(),
                content.getPublishedAt(),
                content.getUpdatedAt());
    }
}
