package vn.io.ajarline.topic.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.topic.contract.TopicCreateInput;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes and validates Topic values.
 *
 * <p>The limits match the database column sizes. The database stays the last
 * line of defense; this policy gives a clear business failure first.</p>
 */
@Component
public class TopicPolicy {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final int SLUG_MAX_LENGTH = 160;
    private static final int NAME_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;

    /**
     * Returns the canonical input accepted by the create operation.
     *
     * <p>The slug is trimmed and lowercased. An empty description becomes null.
     * The server never generates a slug from the name.</p>
     *
     * @throws TopicOperationException when a value violates the Topic limits
     */
    public TopicCreateInput normalizeCreate(TopicCreateInput input) {
        if (input == null) {
            throw failure("Topic content is required.");
        }
        String slug = normalizeSlug(input.slug());
        validateText(slug, "Slug", SLUG_MAX_LENGTH, false);
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw failure("Slug must contain lowercase letters, digits, and single hyphens.");
        }
        validateText(input.name(), "Name", NAME_MAX_LENGTH, false);
        String description = normalizeDescription(input.description());
        validateText(description, "Description", DESCRIPTION_MAX_LENGTH, true);
        return new TopicCreateInput(slug, input.name(), description, input.active());
    }

    private String normalizeSlug(String slug) {
        if (slug == null) {
            throw failure("Slug is required.");
        }
        return slug.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        if (description != null && description.isEmpty()) {
            return null;
        }
        return description;
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

    private TopicOperationException failure(String message) {
        return new TopicOperationException(TopicFailureCode.VALIDATION_FAILED, message);
    }
}
