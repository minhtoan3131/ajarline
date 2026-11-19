package vn.io.ajarline.content.contract;

import java.util.Objects;

/**
 * Proves that one item is locked for editing in this transaction, at the expected revision.
 * It carries the state read under the lock. Pass it back to {@link ContentRecordWriter#replace}.
 */
public record EditableRecord(ContentRecordView current) {

    public EditableRecord {
        Objects.requireNonNull(current, "current");
    }
}
