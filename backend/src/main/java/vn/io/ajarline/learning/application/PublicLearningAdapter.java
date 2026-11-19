package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.learning.contract.LearningVisibility;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Answers what readers can see in Series and Modules. Other capabilities ask through ports.
 * Every method must run in the caller's transaction. A second transaction would show
 * two different states in one request.
 */
@Component
public class PublicLearningAdapter implements LearningVisibility {
    private final PublicHierarchyReader reader;
    private final PublicHierarchyPolicy policy;

    public PublicLearningAdapter(PublicHierarchyReader reader, PublicHierarchyPolicy policy) {
        this.reader = reader;
        this.policy = policy;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<UUID> publicModuleIds(Collection<UUID> moduleIds) {
        if (moduleIds.isEmpty()) {
            return Set.of();
        }
        PublicHierarchy hierarchy = policy.judge(reader.readDown(moduleIds));
        return moduleIds.stream()
                .filter(hierarchy::isPublic)
                .collect(Collectors.toUnmodifiableSet());
    }
}
