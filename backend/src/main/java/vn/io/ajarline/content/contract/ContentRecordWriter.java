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
     * Replaces the text fields and Topics of a locked item. The revision rises by one when
     * anything changed. The input must be the one given to {@link #lockForEdit}.
     *
     * @param subtypeChanged true when the caller changed its own part of the item. The revision
     *                       then rises even if the common record did not change.
     */
    ContentRecordView replace(EditableRecord record, ContentRecordInput input, boolean subtypeChanged);
}
