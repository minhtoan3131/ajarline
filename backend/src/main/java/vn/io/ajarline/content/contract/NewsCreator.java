package vn.io.ajarline.content.contract;

public interface NewsCreator {

    /**
     * Creates a draft News item at revision 1.
     *
     * @throws ContentOperationException with {@code VALIDATION_FAILED}, {@code SLUG_CONFLICT},
     *         {@code TOPIC_REFERENCE_NOT_FOUND} or {@code INACTIVE_TOPIC_ASSIGNMENT}
     */
    NewsView create(NewsContentInput input);
}
