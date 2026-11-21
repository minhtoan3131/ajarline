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
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleEditor;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleRemover;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.platform.web.OpenApiConfiguration;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Admin Content", description = "Authenticated Article authoring operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/articles")
public class AdminArticleController {
    private static final String BASE_PATH = "/api/admin/v1/articles";
    private final ArticleCreator creator;
    private final ArticleReader reader;
    private final ArticleEditor editor;
    private final ArticleRemover remover;

    public AdminArticleController(ArticleCreator creator, ArticleReader reader, ArticleEditor editor,
                                  ArticleRemover remover) {
        this.creator = creator;
        this.reader = reader;
        this.editor = editor;
        this.remover = remover;
    }


    @Operation(
            operationId = "createArticle",
            summary = "Create an Article")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Article created",
                    useReturnTypeSchema = true,
                    headers = {
                            @Header(name = "Location", description = "Created Admin Article URL"),
                            @Header(name = "ETag", description = "Strong decimal Article revision")
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
                    description = "Article slug conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Article validation failed")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArticleAdminDetail> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ArticleEditable.class)))
            @RequestBody(required = false) JsonNode body) {
        ArticleEditable editable = ArticleEditable.parse(body);
        ArticleView created = creator.create(editable.toContentInput());
        URI location = URI.create(BASE_PATH + "/" + created.id());
        return ResponseEntity.created(location)
                .eTag(RevisionHttpHeaders.etag(created.revision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleAdminDetail.from(created));
    }

    @Operation(
            operationId = "getAdminArticle",
            summary = "Get an Article for Admin editing")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Article returned",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Strong decimal Article revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Article id is not a valid UUID"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Article was not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArticleAdminDetail> get(@PathVariable UUID id) {
        ArticleView article = reader.get(id);
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(article.revision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleAdminDetail.from(article));

    }


    @Operation(
            operationId = "updateArticle",
            summary = "Replace the editable state of an Article")
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
                    description = "Article updated",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal Article revision")),
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
                    description = "Article was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Article slug conflicts or is immutable"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Article validation failed"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArticleAdminDetail> update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ArticleEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        ArticleEditable editable = ArticleEditable.parse(body);
        ArticleView updated = editor.replace(id, expectedRevision, editable.toContentInput());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(updated.revision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleAdminDetail.from(updated));
    }

    @Operation(
            operationId = "deleteArticle",
            summary = "Hard-delete a never-published Draft Article")
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
                    description = "Article hard-deleted"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Article id is not a valid UUID, or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Article was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Article was published once, so it cannot be deleted"),
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
}
