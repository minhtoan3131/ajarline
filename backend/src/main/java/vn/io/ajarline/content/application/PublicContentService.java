package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PublicContentService implements PublicContentQuery {
    private static final PublicationStatus PUBLIC_STATUS = PublicationStatus.PUBLISHED;

    private final ContentItemJpaRepository contentRepository;
    private final TopicAssignmentPort topicAssignments;

    public PublicContentService(
            ContentItemJpaRepository contentRepository,
            TopicAssignmentPort topicAssignments) {
        this.contentRepository = contentRepository;
        this.topicAssignments = topicAssignments;
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
        requireValidPaging(page, size);
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

    /**
     * Returns one page of the public Articles that belong to no Module,
     * ordered by title and then by id, each with its Topics.
     *
     * <p>Paging is applied after filtering and ordering. A page after the
     * last one is empty. The Topics of the whole page are read once.</p>
     *
     * @param page zero-based page number, at least 0
     * @param size number of Articles per page, at least 1
     */
    @Transactional(readOnly = true)
    public StandaloneArticlePage findStandaloneArticles(int page, int size) {
        requireValidPaging(page, size);

        // NOTE: Until Modules exist, every public Article is standalone. When
        // the learning capability can say which Articles belong to a Module,
        // exclude them here through LearningContextPort, in the query and
        // before paging, so the Topic Home group and this list agree.
        List<ContentItemEntity> window = contentRepository.findWindowOfTypeWithStatus(
                ContentType.ARTICLE.name(), PUBLIC_STATUS.name(), size + 1, (long) page * size);

        List<PublicContentSummary> articles = window.stream()
                .limit(size)
                .map(this::toSummary)
                .toList();
        if (articles.isEmpty()) {
            return StandaloneArticlePage.empty();
        }

        List<UUID> articleIds = articles.stream().map(PublicContentSummary::id).toList();
        Map<UUID, List<TopicLabel>> topics = topicAssignments.topicLabelsOf(articleIds);
        List<StandaloneArticlePage.Entry> entries = articles.stream()
                .map(article -> new StandaloneArticlePage.Entry(
                        article, topics.getOrDefault(article.id(), List.of())))
                .toList();
        return new StandaloneArticlePage(entries, window.size() > size);
    }

    private static void requireValidPaging(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be at least 0 and size at least 1.");
        }
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
