package vn.io.ajarline.topic.contract;

/**
 * Carries the values needed to create a Topic.
 *
 * <p>The values are not validated here. The Topic capability normalizes and
 * validates them before saving.</p>
 */
public record TopicCreateInput(
        String slug,
        String name,
        String description,
        boolean active
) {
}
