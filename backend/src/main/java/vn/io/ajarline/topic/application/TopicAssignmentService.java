package vn.io.ajarline.topic.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.topic.infrastructure.persistence.TopicEntity;
import vn.io.ajarline.topic.infrastructure.persistence.TopicJpaRepository;
import vn.io.ajarline.topic.infrastructure.persistence.TopicLabelRow;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TopicAssignmentService implements TopicAssignmentPort {

    private final TopicJpaRepository topics;

    public TopicAssignmentService(TopicJpaRepository topics) {
        this.topics = topics;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> topicIdsOf(UUID contentId) {
        return topics.findAssignedTopicIds(contentId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<TopicLabel> topicLabelsOf(UUID contentId) {
        return topics.findLabelsOfContent(List.of(contentId)).stream()
                .map(TopicLabelRow::toLabel)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<UUID, List<TopicLabel>> topicLabelsOf(Collection<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        // The rows come ordered by name and id, and grouping keeps that order for each item.
        return topics.findLabelsOfContent(contentIds).stream()
                .collect(Collectors.groupingBy(
                        TopicLabelRow::getContentId,
                        Collectors.mapping(TopicLabelRow::toLabel, Collectors.toList())));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean replaceAssignments(UUID contentId, List<UUID> topicIds) {
        List<UUID> current = topics.findAssignedTopicIds(contentId);
        Set<UUID> wanted = new HashSet<>(topicIds);
        List<UUID> added = topicIds.stream()
                .filter(id -> !current.contains(id))
                .toList();
        List<UUID> removed = current.stream()
                .filter(id -> !wanted.contains(id))
                .toList();

        if (added.isEmpty() && removed.isEmpty()) {
            return false;
        }

        requireAssignable(added);
        added.forEach(topicId -> topics.insertAssignment(contentId, topicId));
        if (!removed.isEmpty()) {
            topics.deleteAssignments(contentId, removed);
        }
        return true;
    }

    /**
     * Only Topics that are being added are checked. A Topic that is already
     * assigned may stay even after it was turned off.
     */
    private void requireAssignable(List<UUID> addedIds) {
        if (addedIds.isEmpty()) {
            return;
        }
        List<TopicEntity> found = topics.findAllByIdForShare(addedIds);
        if (found.size() != addedIds.size()) {
            throw new ContentOperationException(
                    ContentFailureCode.TOPIC_REFERENCE_NOT_FOUND,
                    "One or more Topic ids do not exist.");
        }
        if (found.stream().anyMatch(topic -> !topic.isActive())) {
            throw new ContentOperationException(
                    ContentFailureCode.INACTIVE_TOPIC_ASSIGNMENT,
                    "An inactive Topic cannot be newly assigned.");
        }
    }
}
