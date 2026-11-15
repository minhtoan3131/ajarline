package vn.io.ajarline.topic.contract;

import java.util.UUID;

public interface TopicRemover {

    /**
     * Deletes a Topic that has no assignments.
     *
     * @throws TopicOperationException with {@link TopicFailureCode#TOPIC_NOT_FOUND}
     *         when no Topic has the id, or {@link TopicFailureCode#TOPIC_IN_USE}
     *         when the Topic is still assigned to content
     */
    void delete(UUID id);
}
