package vn.io.ajarline.content.contract;

import java.util.Objects;
import java.util.UUID;

/**
 * Where an Article sits: the Module and the position in it. A standalone Article has none.
 * The range of the position is checked by the capability that owns the order.
 */
public record ArticlePlacement(UUID moduleId, int position) {

    public ArticlePlacement {
        Objects.requireNonNull(moduleId, "moduleId");
    }
}
