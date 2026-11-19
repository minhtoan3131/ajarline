package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.PublicContentSummary;

/**
 * Holds a public Article in a Module and its rank among the public Articles, from 1.
 */
public record ArticleEntry(PublicContentSummary article, int position) {
}
