package vn.io.ajarline.publication.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;

@Component
public class ArticlePublicationPolicy {

    public void validateForPublish(ArticleView article) {
        requireCanonicalIdentity(article.title(), "title");
        requireCanonicalIdentity(article.slug(), "slug");
        if (isBlank(article.summary())) {
            throw failure(
                    PublicationFailureCode.CONTENT_SUMMARY_REQUIRED,
                    "Article summary is required for publication."
            );
        }
        if (isBlank(article.bodyMarkdown())) {
            throw failure(
                    PublicationFailureCode.ARTICLE_BODY_REQUIRED,
                    "Article body is required for publication."
            );
        }
    }

    private void requireCanonicalIdentity(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalStateException("Canonical Article " + field + " must not be blank.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PublicationOperationException failure(PublicationFailureCode code, String message) {
        return new PublicationOperationException(code, message);
    }

}
