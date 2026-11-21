package vn.io.ajarline.retrieval.contract;

import java.util.UUID;

public interface ConceptRemover {

    /**
     * Deletes a Concept and all its expressions, and raises the vocabulary version by one.
     *
     * @throws ConceptOperationException with {@link ConceptFailureCode#CONCEPT_NOT_FOUND}
     *         when no Concept has the id
     */
    void delete(UUID id);
}
