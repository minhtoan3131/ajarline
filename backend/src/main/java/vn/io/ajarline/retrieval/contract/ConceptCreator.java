package vn.io.ajarline.retrieval.contract;

public interface ConceptCreator {

    /**
     * Creates a Concept with its expressions and raises the vocabulary version by one.
     *
     * @throws ConceptOperationException with {@link ConceptFailureCode#VALIDATION_FAILED}
     *         when a value is invalid, {@link ConceptFailureCode#DUPLICATE_CONCEPT_EXPRESSION}
     *         when two expressions are the same after normalization, or
     *         {@link ConceptFailureCode#CONCEPT_KEY_CONFLICT} when the key is already used
     */
    ConceptView create(ConceptCreateInput input);
}
