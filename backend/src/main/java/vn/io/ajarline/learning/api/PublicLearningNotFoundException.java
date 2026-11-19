package vn.io.ajarline.learning.api;

/**
 * Tells that a Series or a Module is not publicly available. The reason is never given:
 * an unknown slug, a draft, an unpublished item and an item with no public child look the same.
 */
final class PublicLearningNotFoundException extends RuntimeException {
    private final Kind kind;

    PublicLearningNotFoundException(Kind kind) {
        super(kind.label() + " is not publicly available.");
        this.kind = kind;
    }

    Kind kind() {
        return kind;
    }

    enum Kind {
        SERIES("Series"),
        MODULE("Module");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
