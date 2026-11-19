package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Holds the public identity of a Series shown with other content.
 */
public record PublicSeriesRef(UUID id, String slug, String title) {
}
