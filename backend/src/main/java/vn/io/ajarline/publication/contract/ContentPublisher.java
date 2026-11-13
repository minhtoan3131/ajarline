package vn.io.ajarline.publication.contract;

import java.util.UUID;

public interface ContentPublisher {
    PublicationResult publish(UUID id, long expectedRevision);
}
