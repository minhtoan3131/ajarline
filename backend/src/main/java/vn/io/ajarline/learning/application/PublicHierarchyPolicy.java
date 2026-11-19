package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Holds the rules about what readers can see in Series, Modules and Articles.
 * It uses values that are already read, so it does no I/O.
 *
 * <p>A Module is public when it is published and has a public Article. A Series is public
 * when it is published and has a public Module. This is the only place that says so.</p>
 */
@Component
public class PublicHierarchyPolicy {

    /**
     * Decides which Modules and Series readers can see. A Module is decided first, because a Series depends on it.
     */
    public PublicHierarchy judge(HierarchyFacts facts) {
        Map<UUID, ContentType> types = new HashMap<>();
        Map<UUID, UUID> parents = new HashMap<>();
        facts.byId().forEach((id, fact) -> {
            types.put(id, fact.type());
            fact.childIds().forEach(childId -> parents.put(childId, id));
        });

        Set<UUID> publicIds = new HashSet<>();
        Map<UUID, List<UUID>> publicChildren = new HashMap<>();

        facts.byId().forEach((id, fact) -> {
            if (fact.type() != ContentType.MODULE || fact.status() != PublicationStatus.PUBLISHED) {
                return;
            }
            List<UUID> articles = fact.childIds().stream()
                    .filter(childId -> isPublished(facts, childId))
                    .toList();
            if (!articles.isEmpty()) {
                publicIds.add(id);
                publicChildren.put(id, articles);
            }
        });

        facts.byId().forEach((id, fact) -> {
            if (fact.type() != ContentType.SERIES) {
                return;
            }
            List<UUID> modules = fact.childIds().stream()
                    .filter(publicIds::contains)
                    .toList();
            if (modules.isEmpty()) {
                return;
            }
            // The list is kept even when the Series is hidden, so its Modules still have a rank.
            publicChildren.put(id, modules);
            if (fact.status() == PublicationStatus.PUBLISHED) {
                publicIds.add(id);
            }
        });

        return new PublicHierarchy(types, publicIds, publicChildren, parents);
    }

    /**
     * Numbers the public children of a parent from 1. A hidden child leaves no gap.
     */
    public List<RankedChild> rankedChildren(PublicHierarchy hierarchy, UUID parentId) {
        List<UUID> ids = hierarchy.publicChildrenOf(parentId);
        return IntStream.range(0, ids.size())
                .mapToObj(index -> new RankedChild(ids.get(index), index + 1))
                .toList();
    }

    /**
     * Finds a public child with its nearest public neighbours in the same parent.
     * It gives nothing when the child is not a public child of the parent.
     */
    public Optional<Siblings> siblingsOf(PublicHierarchy hierarchy, UUID parentId, UUID childId) {
        List<RankedChild> ranked = rankedChildren(hierarchy, parentId);
        for (int index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).id().equals(childId)) {
                RankedChild previous = index == 0 ? null : ranked.get(index - 1);
                RankedChild next = index == ranked.size() - 1 ? null : ranked.get(index + 1);
                return Optional.of(new Siblings(ranked.get(index), previous, next));
            }
        }
        return Optional.empty();
    }

    private static boolean isPublished(HierarchyFacts facts, UUID id) {
        HierarchyFacts.Fact fact = facts.byId().get(id);
        return fact != null && fact.status() == PublicationStatus.PUBLISHED;
    }

    /**
     * Holds a public child and its rank. The rank counts public children only, from 1.
     */
    public record RankedChild(UUID id, int position) {
    }

    /**
     * Holds a public child and its public neighbours. A neighbour is null at the start or the end.
     */
    public record Siblings(RankedChild self, RankedChild previous, RankedChild next) {
    }
}
