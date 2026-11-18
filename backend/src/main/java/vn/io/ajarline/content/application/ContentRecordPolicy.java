package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ContentRecordPolicy {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public void requireValidTitle(String title) {
        validateText(title, "Title", 300, false);
    }

    public String normalizeSlug(String slug) {
        if (slug == null) {
            throw validationFailure("Slug is required.");
        }
        String normalized = slug.strip().toLowerCase(Locale.ROOT);
        validateText(normalized, "Slug", 180, false);
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw validationFailure("Slug must contain lowercase letters, digits, and single hyphens.");
        }
        return normalized;
    }

    public String normalizeSummary(String summary) {
        String normalized = summary != null && summary.isEmpty() ? null : summary;
        validateText(normalized, "Summary", 1000, true);
        return normalized;
    }

    public void requirePublishableSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            throw new ContentOperationException(
                    ContentFailureCode.CONTENT_SUMMARY_REQUIRED,
                    "A summary is required for published content."
            );
        }
    }

    private void validateText(String value, String fieldName, int maximumLength, boolean nullable) {
        if (value == null) {
            if (nullable) return;
            throw validationFailure(fieldName + " is required.");
        }
        if (value.isBlank()) {
            throw validationFailure(fieldName + " must not be blank.");
        }
        if (value.codePointCount(0, value.length()) > maximumLength) {
            throw validationFailure(fieldName + " must not exceed " + maximumLength + " characters.");
        }
    }

    private ContentOperationException validationFailure(String message) {
        return new ContentOperationException(ContentFailureCode.VALIDATION_FAILED, message);
    }
}
