package vn.io.ajarline.retrieval.contract;

import java.util.UUID;

public interface ConceptEditor {

    /**
     * Replaces the editable values and the whole expression set of a Concept.
     *
     * <p>When nothing differs from the stored Concept, nothing is written, the version
     * does not change and {@code updatedAt} stays. Otherwise the vocabulary version
     * rises by one.</p>
     *
     * @throws ConceptOperationException with {@link ConceptFailureCode#VALIDATION_FAILED}
     *         or {@link ConceptFailureCode#DUPLICATE_CONCEPT_EXPRESSION} when a value is
     *         invalid, or {@link ConceptFailureCode#CONCEPT_NOT_FOUND} when no Concept has the id
     */
    ConceptView replace(UUID id, ConceptUpdateInput input);
}
