package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Creates and replaces the common record of content items of any type.
 *
 * <p>Content owns the record, its slug, its Topics and its revision. A capability that owns the
 * rest of an item calls this port in the same transaction. Methods that touch the database need
 * the caller's transaction.</p>
 */
public interface ContentRecordWriter {

    /**
     * Checks the text fields and returns them in canonical form. It does no database access.
     * An empty summary becomes {@code null}.
     *
     * @throws ContentOperationException with {@link ContentFailureCode#VALIDATION_FAILED}
     *         when a field breaks a limit or the Topic ids repeat
     */
    ContentRecordInput normalize(ContentRecordInput input);

    /**
     * Creates a draft at revision 1 and assigns its Topics. The input must come from {@link #normalize}.
     *
     * @throws ContentOperationException with {@link ContentFailureCode#SLUG_CONFLICT}
     *         when the type already has an item with this slug
     */
    ContentRecordView create(ContentType type, ContentRecordInput input);

    /**
     * Locks one item for editing. The input must come from {@link #normalize}.
     *
     * <p>It checks in this order: the item exists with this type, the revision matches, the slug
     * is unchanged after first publication, and the summary is present if the item is published.</p>
     */
    EditableRecord lockForEdit(
            UUID id, ContentType type, long expectedRevision, ContentRecordInput input);

    /**
     * Locks one item and checks its revision. Use it when the caller changes only its own part
     * of the item, for example the order of its children, so there is no new text to check.
     *
     * @throws ContentOperationException with {@link ContentFailureCode#CONTENT_NOT_FOUND}
     *         when the id is unknown or the item has another type, or with
     *         {@link ContentFailureCode#REVISION_CONFLICT} when the revision is not current
     */
    EditableRecord lockForEdit(UUID id, ContentType type, long expectedRevision);

    /**
     * Replaces the text fields and Topics of a locked item. The revision rises by one when
     * anything changed. The input must be the one given to {@link #lockForEdit}.
     *
     * @param subtypeChanged true when the caller changed its own part of the item. The revision
     *                       then rises even if the common record did not change.
     */
    ContentRecordView replace(EditableRecord record, ContentRecordInput input, boolean subtypeChanged);

    /**
     * Locks the row of an item that owns an ordered list of children. It checks no revision.
     * The lock lasts until the transaction ends.
     *
     * <p>A caller that locks several items must use the same order everywhere, for example
     * ascending id. Then two callers cannot wait for each other.</p>
     *
     * @return true when the item exists with this type and is now locked
     */
    boolean lockStructure(UUID id, ContentType type);

    /**
     * Locks one item for deletion and checks it in this order: the item exists with this type,
     * the revision matches, and the item was never published.
     *
     * @throws ContentOperationException with {@link ContentFailureCode#CONTENT_NOT_FOUND}
     *         when the id is unknown or the item has another type, with
     *         {@link ContentFailureCode#REVISION_CONFLICT} when the revision is not current,
     *         or with {@link ContentFailureCode#CONTENT_ALREADY_EVER_PUBLISHED} when the item
     *         was published once, even if it is unpublished now
     */
    DeletableRecord lockForDelete(UUID id, ContentType type, long expectedRevision);

    /**
     * Deletes the row of a locked item and writes the change to the database at once. The
     * database removes the rows of the item's own parts and its Topic links. A database failure
     * is raised here.
     */
    void deleteRecord(DeletableRecord record);
}
