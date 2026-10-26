package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ArticlePolicy {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    /**
     * Returns canonical input accepted by Article content operations.
     *
     * @throws ContentOperationException when an input field violates the
     *         content contract
     */
    public ArticleContentInput normalizeAndValidate(ArticleContentInput input) {
        if (input == null) {
            throw failure("Article content is required.");
        }
        validateText(input.title(), "Title", 300, false);
        String slug = normalizeSlug(input.slug());
        validateText(slug, "Slug", 180, false);
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw failure("Slug must contain lowercase letters, digits, and single hyphens.");
        }
        String summary = input.summary();
        if (summary != null && summary.isEmpty()) {
            summary = null;
        }
        validateText(summary, "Summary", 1000, true);
        return new ArticleContentInput(input.title(), slug, summary, input.bodyMarkdown());
    }

    private void validateText(String value, String fieldName, int maximumLength, boolean nullable) {
        if (value == null) {
            if (nullable) {
                return;
            }
            throw failure(fieldName + " is required.");
        }
        if (value.isBlank()) {
            throw failure(fieldName + " must not be blank.");
        }
        int length = value.codePointCount(0, value.length());
        if (length > maximumLength) {
            throw failure(fieldName + " must not exceed " + maximumLength + " characters.");
        }
    }


    private ContentOperationException failure(String message) {
        return new ContentOperationException(ContentFailureCode.VALIDATION_FAILED, message);
    }

    private String normalizeSlug(String slug) {
        if (slug == null) {
            throw failure("Slug is required.");
        }
        return slug.strip().toLowerCase(Locale.ROOT);
    }
}
