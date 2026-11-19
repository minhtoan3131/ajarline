package vn.io.ajarline.content.contract;

import java.time.Instant;

/**
 * The outside source of a News item. Every part may be null in a draft.
 *
 * @param publishedAt when the source published the story; never the Ajarline publish time
 */
public record NewsSource(String name, String url, Instant publishedAt) {
}
