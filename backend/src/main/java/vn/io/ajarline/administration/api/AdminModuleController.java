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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.learning.contract.ArticleOrderEditor;
import vn.io.ajarline.learning.contract.ChildOrderView;
import vn.io.ajarline.learning.contract.ModuleCreator;
import vn.io.ajarline.learning.contract.ModuleEditor;
import vn.io.ajarline.learning.contract.ModuleReader;
import vn.io.ajarline.learning.contract.ModuleRemover;
import vn.io.ajarline.learning.contract.ModuleView;
import vn.io.ajarline.platform.web.OpenApiConfiguration;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Admin Content", description = "Authenticated Module authoring operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/modules")
public class AdminModuleController {
    private static final String BASE_PATH = "/api/admin/v1/modules";
    private final ModuleCreator creator;
    private final ModuleReader reader;
    private final ModuleEditor editor;
    private final ArticleOrderEditor orderEditor;
    private final ModuleRemover remover;

    public AdminModuleController(ModuleCreator creator, ModuleReader reader, ModuleEditor editor,
                                 ArticleOrderEditor orderEditor, ModuleRemover remover) {
        this.creator = creator;
        this.reader = reader;
        this.editor = editor;
        this.orderEditor = orderEditor;
        this.remover = remover;
    }

    @Operation(
            operationId = "createModule",
            summary = "Create a Draft Module in a Series")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Module created as DRAFT revision 1",
                    useReturnTypeSchema = true,
                    headers = {
                            @Header(name = "Location", description = "Created Admin Module URL"),
                            @Header(name = "ETag", description = "Strong decimal Module revision")
                    }),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request shape, JSON or UUID is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Module slug conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Module validation failed, the Series does not exist, or the position is invalid")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ModuleAdminDetail> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ModuleEditable.class)))
            @RequestBody(required = false) JsonNode body) {
        ModuleEditable editable = ModuleEditable.parse(body);
        ModuleView created = creator.create(editable.toContentInput());
        URI location = URI.create(BASE_PATH + "/" + created.record().id());
        return ResponseEntity.created(location)
                .eTag(RevisionHttpHeaders.etag(created.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(ModuleAdminDetail.from(created));
    }

    @Operation(
            operationId = "getAdminModule",
            summary = "Get a Module for Admin editing")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Module returned with its Articles in order",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Strong decimal Module revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Module id is not a valid UUID"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Module was not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ModuleAdminDetail> get(@PathVariable UUID id) {
        ModuleView module = reader.get(id);
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(module.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(ModuleAdminDetail.from(module));
    }

    @Operation(
            operationId = "updateModule",
            summary = "Replace the editable state of a Module, including its place")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision returned in ETag, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Module updated",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal Module revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request shape, JSON, UUID or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Module was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Module slug conflicts or is immutable"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Module validation failed, the Series does not exist, or the position is invalid"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ModuleAdminDetail> update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ModuleEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        ModuleEditable editable = ModuleEditable.parse(body);
        ModuleView updated = editor.replace(id, expectedRevision, editable.toContentInput());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(updated.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(ModuleAdminDetail.from(updated));
    }

    @Operation(
            operationId = "deleteModule",
            summary = "Hard-delete a never-published Draft Module that has no Article")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision returned in ETag, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Module hard-deleted"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Module id is not a valid UUID, or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Module was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Module was published once (CONTENT_ALREADY_EVER_PUBLISHED), "
                            + "or still has an Article (CONTENT_IN_USE)"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        remover.delete(id, expectedRevision);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @Operation(
            operationId = "reorderArticles",
            summary = "Replace the order of all Articles of a Module")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision of the Module, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order saved, or already as requested",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal Module revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request shape, JSON, UUID or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Module was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "The ids are not exactly the current Articles of the Module"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "An id appears more than once"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PutMapping(
            value = "/{moduleId}/article-order",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArticleOrderResponse> reorderArticles(
            @PathVariable UUID moduleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ArticleOrderEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        ArticleOrderEditable editable = ArticleOrderEditable.parse(body);
        ChildOrderView order = orderEditor.reorderArticles(
                moduleId, expectedRevision, editable.articleIds());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(order.parentRevision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleOrderResponse.from(order));
    }
}
