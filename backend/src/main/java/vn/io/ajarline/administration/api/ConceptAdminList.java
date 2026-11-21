package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.retrieval.contract.ConceptView;

import java.util.List;

/**
 * Represents the Concept list returned by the Admin API. The list is not paged.
 */
@Schema(requiredProperties = {"concepts"})
public record ConceptAdminList(List<ConceptAdminItem> concepts) {

    static ConceptAdminList from(List<ConceptView> views) {
        return new ConceptAdminList(views.stream().map(ConceptAdminItem::from).toList());
    }
}
