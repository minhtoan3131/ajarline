package vn.io.ajarline.content.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads and changes the Topics assigned to one content item.
 *
 * <p>Content defines this port and the Topic capability implements it, so
 * content never imports Topic. Every method must run inside the caller's
 * transaction: an assignment change is part of saving the content item and
 * must commit or roll back together with it.</p>
 */
public interface TopicAssignmentPort {

    /**
     * Returns the ids of the Topics assigned to the content item, ordered by id.
     */
    List<UUID> topicIdsOf(UUID contentId);

    /**
     * Returns the Topics assigned to the content item, ordered by name and
     * then by id. Inactive Topics are included: turning a Topic off does not
     * remove it from content.
     */
    List<TopicLabel> topicLabelsOf(UUID contentId);

    /**
     * Returns the Topics assigned to each of the given content items with one
     * read. The labels of an item are ordered by name and then by id, and
     * inactive Topics are included. An item that has no Topic has no key in the
     * result, so callers should use {@code getOrDefault(id, List.of())}.
     *
     * <p>An empty collection returns an empty map without any read.</p>
     */
    Map<UUID, List<TopicLabel>> topicLabelsOf(Collection<UUID> contentIds);

    /**
     * Makes the Topics assigned to the content item equal to the given ids.
     * Only the difference is written: new Topics are added, missing ones are
     * removed.
     *
     * <p>The ids must not repeat. A Topic that is already assigned may stay
     * even when it has been turned off. A Topic that is turned off cannot be
     * newly assigned.</p>
     *
     * @return true when at least one assignment was added or removed
     * @throws ContentOperationException with
     *         {@link ContentFailureCode#TOPIC_REFERENCE_NOT_FOUND} when a new
     *         id names no Topic, or
     *         {@link ContentFailureCode#INACTIVE_TOPIC_ASSIGNMENT} when a new
     *         id names an inactive Topic
     */
    boolean replaceAssignments(UUID contentId, List<UUID> topicIds);
}
