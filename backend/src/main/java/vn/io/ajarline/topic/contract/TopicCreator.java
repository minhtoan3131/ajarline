package vn.io.ajarline.topic.contract;

public interface TopicCreator {

    /**
     * Creates a Topic.
     *
     * @throws TopicOperationException with {@link TopicFailureCode#VALIDATION_FAILED}
     *         when a value is invalid, or {@link TopicFailureCode#SLUG_CONFLICT}
     *         when the slug is already used
     */
    TopicView create(TopicCreateInput input);
}
