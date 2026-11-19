package vn.io.ajarline.topic.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.topic.infrastructure.persistence.TopicEntity;
import vn.io.ajarline.topic.infrastructure.persistence.TopicJpaRepository;
import vn.io.ajarline.topic.infrastructure.persistence.TopicLabelRow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs the anonymous Topic browse use cases.
 *
 * <p>A Topic is public when at least one content item that is public now is
 * directly assigned to it. The active flag plays no part here. Content
 * decides which items are public; this service only knows the
 * assignments.</p>
 */
@Service
public class PublicTopicService {

    private final TopicJpaRepository topics;
    private final PublicContentQuery publicContent;

    public PublicTopicService(TopicJpaRepository topics, PublicContentQuery publicContent) {
        this.topics = topics;
        this.publicContent = publicContent;
    }

    /**
     * Returns the public Topics, ordered by name and then by id.
     */
    @Transactional(readOnly = true)
    public List<TopicLabel> listPublicTopics() {
        Set<UUID> publicContentIds = publicContent.findPublicIds(topics.findAllAssignedContentIds());
        if (publicContentIds.isEmpty()) {
            return List.of();
        }
        return topics.findAssignedToAnyContent(publicContentIds).stream()
                .map(topic -> new TopicLabel(topic.getId(), topic.getSlug(), topic.getName()))
                .toList();
    }

    /**
     * Returns the Topic when it is public. A missing Topic and a Topic with
     * no public content give the same empty result.
     */
    @Transactional(readOnly = true)
    public Optional<PublicTopicView> findPublicTopic(String slug) {
        return findPublic(slug).map(found -> toView(found.topic()));
    }

    /**
     * Returns one page of the public content directly assigned to the Topic,
     * or an empty result when the Topic is not public.
     */
    @Transactional(readOnly = true)
    public Optional<PublicTopicContent> findPublicContent(
            String slug, Set<ContentType> types, int page, int size) {
        return findPublic(slug).map(found -> {
            PublicContentPage contentPage = publicContent.findPublicPage(
                    found.assignedContentIds(), types, page, size);
            Map<UUID, List<TopicLabel>> labels = labelsByContent(contentPage.items());
            List<PublicTopicContent.Entry> entries = contentPage.items().stream()
                    .map(item -> new PublicTopicContent.Entry(
                            item, labels.getOrDefault(item.id(), List.of())))
                    .toList();
            return new PublicTopicContent(entries, contentPage.hasMore());
        });
    }

    private Optional<PublicTopic> findPublic(String slug) {
        return topics.findBySlug(slug)
                .map(topic -> new PublicTopic(topic, topics.findAssignedContentIds(topic.getId())))
                .filter(found -> !publicContent.findPublicIds(found.assignedContentIds()).isEmpty());
    }

    /**
     * Reads the Topics of every item on the page with one query. Rows come
     * ordered by Topic name and id, and grouping keeps that order.
     */
    private Map<UUID, List<TopicLabel>> labelsByContent(List<PublicContentSummary> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<UUID> contentIds = items.stream().map(PublicContentSummary::id).toList();
        return topics.findLabelsOfContent(contentIds).stream()
                .collect(Collectors.groupingBy(
                        TopicLabelRow::getContentId,
                        Collectors.mapping(TopicLabelRow::toLabel, Collectors.toList())));
    }

    private PublicTopicView toView(TopicEntity topic) {
        return new PublicTopicView(
                topic.getId(),
                topic.getSlug(),
                topic.getName(),
                topic.getDescription());
    }

    private record PublicTopic(TopicEntity topic, List<UUID> assignedContentIds) {
    }
}
