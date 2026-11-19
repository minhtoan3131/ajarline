package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;

import java.util.Collection;
import java.util.UUID;

/**
 * Rules about the children of a Module or a Series that must hold before it is published.
 * The rules use values that are already read, so this class does no I/O.
 * It reports failures with a code. Which Modules readers can see is decided by learning.
 */
@Component
public class StructurePublicationPolicy {

    public void requireModulePublishable(Collection<PublicationStatus> articleStatuses) {
        if (articleStatuses.stream().noneMatch(status -> status == PublicationStatus.PUBLISHED)) {
            throw new PublicationOperationException(
                    PublicationFailureCode.MODULE_HAS_NO_PUBLIC_ARTICLE,
                    "A Module needs at least one published Article.");
        }
    }

    public void requireSeriesPublishable(Collection<UUID> publicModuleIds) {
        if (publicModuleIds.isEmpty()) {
            throw new PublicationOperationException(
                    PublicationFailureCode.SERIES_HAS_NO_PUBLIC_MODULE,
                    "A Series needs at least one Module that readers can see.");
        }
    }
}
