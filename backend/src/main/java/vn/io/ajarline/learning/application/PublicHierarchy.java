package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.ContentType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Holds the decision about what readers can see. A public Module or Series always has at
 * least one public child, and its children keep their stored order.
 *
 * <p>A Series that readers cannot see still keeps its list of public Modules. Then a public
 * Module under it has a rank. Use {@link #publicParentOf} to know whether a parent is visible.</p>
 */
public record PublicHierarchy(
        Map<UUID, ContentType> types,
        Set<UUID> publicIds,
        Map<UUID, List<UUID>> publicChildren,
        Map<UUID, UUID> parents
) {

    public PublicHierarchy {
        types = Map.copyOf(types);
        publicIds = Set.copyOf(publicIds);
        publicChildren = Map.copyOf(publicChildren);
        parents = Map.copyOf(parents);
    }

    /**
     * Tells whether a Module or Series is public. Any other id gives false.
     */
    public boolean isPublic(UUID id) {
        return publicIds.contains(id);
    }

    public Optional<ContentType> typeOf(UUID id) {
        return Optional.ofNullable(types.get(id));
    }

    /**
     * Returns the public children of a Module or Series in stored order. Any other id gives an empty list.
     */
    public List<UUID> publicChildrenOf(UUID parentId) {
        return publicChildren.getOrDefault(parentId, List.of());
    }

    /**
     * Returns the stored parent, whether readers can see it or not.
     */
    public Optional<UUID> parentOf(UUID childId) {
        return Optional.ofNullable(parents.get(childId));
    }

    /**
     * Returns the parent only when readers can see it and it lists the child as public.
     */
    public Optional<UUID> publicParentOf(UUID childId) {
        return parentOf(childId)
                .filter(parentId -> isPublic(parentId) && publicChildrenOf(parentId).contains(childId));
    }
}
