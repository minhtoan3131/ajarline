package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentContext;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicLearningContextPort;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;
import vn.io.ajarline.content.infrastructure.persistence.PublishedIdRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PublicContentService implements PublicContentQuery {
    private static final PublicationStatus PUBLIC_STATUS = PublicationStatus.PUBLISHED;

    private final ContentItemJpaRepository contentRepository;
    private final TopicAssignmentPort topicAssignments;
    private final PublicLearningContextPort learning;

    public PublicContentService(
            ContentItemJpaRepository contentRepository,
            TopicAssignmentPort topicAssignments,
            PublicLearningContextPort learning) {
        this.contentRepository = contentRepository;
        this.topicAssignments = topicAssignments;
        this.learning = learning;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findPublicIds(Collection<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Set.of();
        }
        return publicIdsAmong(contentIds, EnumSet.allOf(ContentType.class));
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

        // Filter first and cut the page after. Then every page is full and hasMore is right.
        Set<UUID> publicIds = publicIdsAmong(contentIds, types);
        if (publicIds.isEmpty()) {
            return PublicContentPage.empty();
        }

        List<String> typeNames = types.stream().map(Enum::name).toList();
        long offset = (long) page * size;
        // One extra row tells whether a next page exists, without a count.
        List<ContentItemEntity> window = contentRepository.findWindowWithStatus(
                publicIds, PUBLIC_STATUS.name(), typeNames, size + 1, offset);

        List<ContentItemEntity> pageItems = window.stream().limit(size).toList();
        Map<UUID, PublicContentContext> contexts = contextsOf(pageItems);
        List<PublicContentSummary> items = pageItems.stream()
                .map(item -> toSummary(item).withContext(
                        contexts.getOrDefault(item.getId(), PublicContentContext.EMPTY)))
                .toList();
        return new PublicContentPage(items, window.size() > size);
    }

    /**
     * Returns one page of the public Articles that readers see as standalone, ordered by
     * title and then by id, each with its Topics.
     *
     * <p>An Article is standalone when it is in no public Module. This includes an Article of
     * a Module that readers cannot see. Paging is applied after filtering and ordering. A page
     * after the last one is empty. The Topics of the whole page are read once.</p>
     *
     * @param page zero-based page number, at least 0
     * @param size number of Articles per page, at least 1
     */
    @Transactional(readOnly = true)
    public StandaloneArticlePage findStandaloneArticles(int page, int size) {
        requireValidPaging(page, size);

        // Articles of a public Module are left out in the query, before paging.
        // Then every page is full and hasMore is right.
        Set<UUID> inPublicModules = learning.articleIdsInPublicModules();
        long offset = (long) page * size;
        List<ContentItemEntity> window = inPublicModules.isEmpty()
                ? contentRepository.findWindowOfTypeWithStatus(
                        ContentType.ARTICLE.name(), PUBLIC_STATUS.name(), size + 1, offset)
                : contentRepository.findWindowOfTypeWithStatusExcludingIds(
                        ContentType.ARTICLE.name(), PUBLIC_STATUS.name(), inPublicModules, size + 1, offset);

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

    /**
     * Keeps the published items of the given types. A Module or a Series stays only when
     * readers can see it, as learning decides.
     */
    private Set<UUID> publicIdsAmong(Collection<UUID> contentIds, Set<ContentType> types) {
        Set<UUID> publicIds = new HashSet<>();
        List<UUID> learningIds = new ArrayList<>();
        for (PublishedIdRow row : contentRepository.findIdsAndTypesWithStatus(contentIds, PUBLIC_STATUS)) {
            ContentType type = row.getContentType();
            if (!types.contains(type)) {
                continue;
            }
            if (type == ContentType.SERIES || type == ContentType.MODULE) {
                learningIds.add(row.getId());
            } else {
                publicIds.add(row.getId());
            }
        }
        if (!learningIds.isEmpty()) {
            publicIds.addAll(learning.publicAmong(learningIds));
        }
        return publicIds;
    }

    /**
     * Reads the learning context of the Articles and Modules on the page with one call.
     * A Series has no parent, so it is not asked.
     */
    private Map<UUID, PublicContentContext> contextsOf(List<ContentItemEntity> items) {
        List<UUID> ids = items.stream()
                .filter(item -> item.getType() == ContentType.ARTICLE || item.getType() == ContentType.MODULE)
                .map(ContentItemEntity::getId)
                .toList();
        return ids.isEmpty() ? Map.of() : learning.contextsOf(ids);
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
                content.getUpdatedAt(),
                PublicContentContext.EMPTY);
    }
}
