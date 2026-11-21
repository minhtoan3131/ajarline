package vn.io.ajarline.retrieval.application;

/**
 * Holds a new Concept after the policy accepted it. The key is trimmed and lowercase.
 */
public record NewConcept(String key, EditableConcept editable) {
}
