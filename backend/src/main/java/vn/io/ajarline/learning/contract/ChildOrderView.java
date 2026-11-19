package vn.io.ajarline.learning.contract;

import java.util.List;
import java.util.UUID;

/**
 * The state of a parent after its children were ordered. It lists every child in order.
 * The revisions are read after the change, so a client can use them as they are.
 */
public record ChildOrderView(UUID parentId, long parentRevision, List<ChildPosition> children) {

    public ChildOrderView {
        children = List.copyOf(children);
    }
}
