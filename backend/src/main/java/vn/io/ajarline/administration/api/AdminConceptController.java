package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.platform.web.OpenApiConfiguration;
import vn.io.ajarline.retrieval.contract.ConceptCreator;
import vn.io.ajarline.retrieval.contract.ConceptReader;
import vn.io.ajarline.retrieval.contract.ConceptView;

import java.net.URI;

@Tag(name = "Admin Retrieval Vocabulary",
        description = "Authenticated retrieval vocabulary operations. A Concept is not a Topic.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/concepts")
public class AdminConceptController {
    private static final String BASE_PATH = "/api/admin/v1/concepts";
    private final ConceptCreator creator;
    private final ConceptReader reader;

    public AdminConceptController(ConceptCreator creator, ConceptReader reader) {
        this.creator = creator;
        this.reader = reader;
    }

    @Operation(
            operationId = "listConcepts",
            summary = "List every Concept with its expressions, active or not")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Concepts returned, ordered by display name and then by id",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConceptAdminList> list() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ConceptAdminList.from(reader.list()));
    }

    @Operation(
            operationId = "createConcept",
            summary = "Create a Concept with its expressions")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Concept created",
                    useReturnTypeSchema = true,
                    headers = @Header(name = "Location", description = "Created Admin Concept URL")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request shape or JSON is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Concept key conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Concept validation failed or two expressions are the same after normalization")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConceptAdminItem> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConceptCreatable.class)))
            @RequestBody(required = false) JsonNode body) {
        ConceptCreatable creatable = ConceptCreatable.parse(body);
        ConceptView created = creator.create(creatable.toInput());
        URI location = URI.create(BASE_PATH + "/" + created.id());
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(ConceptAdminItem.from(created));
    }
}
