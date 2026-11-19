package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface ContentEligibility {
    void requirePublishable(UUID id);
}
