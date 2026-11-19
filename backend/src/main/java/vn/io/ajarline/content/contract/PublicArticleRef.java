package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Holds the public identity of an Article shown next to another Article.
 * The position is the rank of the Article among the public Articles of its Module, from 1.
 */
public record PublicArticleRef(UUID id, String slug, String title, int position) {
}
