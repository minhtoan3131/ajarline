package vn.io.ajarline.retrieval.contract;

import java.util.List;

public interface ConceptReader {

    /**
     * Returns every Concept, active or not, ordered by display name and then by id.
     */
    List<ConceptView> list();
}
