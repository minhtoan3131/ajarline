package vn.io.ajarline.topic.contract;

/**
 * Carries the editable values of an existing Topic. The slug is not part of it
 * because a Topic slug never changes.
 */
public record TopicUpdateInput(
        String name,
        String description,
        boolean active
) {
}
