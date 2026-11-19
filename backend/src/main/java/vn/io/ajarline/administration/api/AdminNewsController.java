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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.NewsCreator;
import vn.io.ajarline.content.contract.NewsEditor;
import vn.io.ajarline.content.contract.NewsReader;
import vn.io.ajarline.content.contract.NewsView;
import vn.io.ajarline.platform.web.OpenApiConfiguration;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Admin Content", description = "Authenticated News authoring operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/news")
public class AdminNewsController {
    private static final String BASE_PATH = "/api/admin/v1/news";
    private final NewsCreator creator;
    private final NewsReader reader;
    private final NewsEditor editor;

    public AdminNewsController(NewsCreator creator, NewsReader reader, NewsEditor editor) {
        this.creator = creator;
        this.reader = reader;
        this.editor = editor;
    }

    @Operation(
            operationId = "createNews",
            summary = "Create a Draft News item")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "News created as DRAFT revision 1",
                    useReturnTypeSchema = true,
                    headers = {
                            @Header(name = "Location", description = "Created Admin News URL"),
                            @Header(name = "ETag", description = "Strong decimal News revision")
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
                    description = "News slug conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "News validation failed")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NewsAdminDetail> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NewsEditable.class)))
            @RequestBody(required = false) JsonNode body) {
        NewsEditable editable = NewsEditable.parse(body);
        NewsView created = creator.create(editable.toContentInput());
        URI location = URI.create(BASE_PATH + "/" + created.record().id());
        return ResponseEntity.created(location)
                .eTag(RevisionHttpHeaders.etag(created.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(NewsAdminDetail.from(created));
    }

    @Operation(
            operationId = "getAdminNews",
            summary = "Get a News item for Admin editing")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "News returned",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Strong decimal News revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "News id is not a valid UUID"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "News was not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NewsAdminDetail> get(@PathVariable UUID id) {
        NewsView news = reader.get(id);
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(news.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(NewsAdminDetail.from(news));
    }

    @Operation(
            operationId = "updateNews",
            summary = "Replace the editable state of a News item")
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
                    description = "News updated, or already as requested",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal News revision")),
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
                    description = "News was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "News slug conflicts or is immutable"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "News validation failed, or a published News item would lose "
                            + "its summary, body or source"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NewsAdminDetail> update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NewsEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        NewsEditable editable = NewsEditable.parse(body);
        NewsView updated = editor.replace(id, expectedRevision, editable.toContentInput());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(updated.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(NewsAdminDetail.from(updated));
    }
}
