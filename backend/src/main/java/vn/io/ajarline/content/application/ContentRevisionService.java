package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRevisionCommands;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Service
public class ContentRevisionService implements ContentRevisionCommands {

    private final ContentItemJpaRepository contentRepository;

    public ContentRevisionService(ContentItemJpaRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void advanceRevisions(Collection<UUID> contentIds, Instant changedAt) {
        if (contentIds.isEmpty()) {
            return;
        }
        contentRepository.advanceRevisions(contentIds, changedAt);
    }
}
