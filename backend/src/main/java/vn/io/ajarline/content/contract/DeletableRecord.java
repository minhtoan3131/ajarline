package vn.io.ajarline.content.contract;

import java.util.Objects;

/**
 * Proves that one item is locked in this transaction, at the expected revision, and was
 * never published. It carries the state read under the lock. Pass it to
 * {@link ContentRecordWriter#deleteRecord}.
 */
public record DeletableRecord(ContentRecordView current) {

    public DeletableRecord {
        Objects.requireNonNull(current, "current");
    }
}
