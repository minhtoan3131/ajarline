package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds what is stored about some Series, Modules and Articles. It holds no decision.
 * An item whose children were not read has an empty child list.
 */
public record HierarchyFacts(Map<UUID, Fact> byId) {

    public HierarchyFacts {
        byId = Map.copyOf(byId);
    }

    /**
     * Holds one item: its type, its own status and its child ids in stored order.
     */
    public record Fact(ContentType type, PublicationStatus status, List<UUID> childIds) {

        public Fact {
            childIds = List.copyOf(childIds);
        }
    }
}
