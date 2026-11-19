package vn.io.ajarline.learning.application;

import vn.io.ajarline.content.contract.PublicContentSummary;

import java.util.List;

/**
 * Holds a public Module in a Series, its rank among the public Modules, from 1, and its public Articles.
 */
public record ModuleEntry(
        PublicContentSummary module,
        int position,
        List<ArticleEntry> articles
) {

    public ModuleEntry {
        articles = List.copyOf(articles);
    }
}
