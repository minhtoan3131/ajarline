package vn.io.ajarline.topic.contract;

import java.util.UUID;

public interface TopicEditor {

    /**
     * Replaces the editable values of a Topic.
     *
     * <p>When no value differs from the stored Topic, nothing is written and
     * {@code updatedAt} does not change.</p>
     *
     * @throws TopicOperationException with {@link TopicFailureCode#VALIDATION_FAILED}
     *         when a value is invalid, or {@link TopicFailureCode#TOPIC_NOT_FOUND}
     *         when no Topic has the id
     */
    TopicView replace(UUID id, TopicUpdateInput input);
}
