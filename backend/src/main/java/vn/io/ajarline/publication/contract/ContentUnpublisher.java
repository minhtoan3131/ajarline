package vn.io.ajarline.publication.contract;

import java.util.UUID;

public interface ContentUnpublisher {

    PublicationResult unpublish(UUID id, long expectedRevision);
}
