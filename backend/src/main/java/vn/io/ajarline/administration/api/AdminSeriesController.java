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
import vn.io.ajarline.learning.contract.SeriesCreator;
import vn.io.ajarline.learning.contract.SeriesEditor;
import vn.io.ajarline.learning.contract.SeriesReader;
import vn.io.ajarline.learning.contract.ChildOrderView;
import vn.io.ajarline.learning.contract.ModuleOrderEditor;
import vn.io.ajarline.learning.contract.SeriesView;
import vn.io.ajarline.platform.web.OpenApiConfiguration;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Admin Content", description = "Authenticated Series authoring operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/series")
public class AdminSeriesController {
    private static final String BASE_PATH = "/api/admin/v1/series";
    private final SeriesCreator creator;
    private final SeriesReader reader;
    private final SeriesEditor editor;
    private final ModuleOrderEditor orderEditor;

    public AdminSeriesController(SeriesCreator creator, SeriesReader reader, SeriesEditor editor,
                                 ModuleOrderEditor orderEditor) {
        this.creator = creator;
        this.reader = reader;
        this.editor = editor;
        this.orderEditor = orderEditor;
    }

    @Operation(
            operationId = "createSeries",
            summary = "Create a Draft Series")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Series created as DRAFT revision 1",
                    useReturnTypeSchema = true,
                    headers = {
                            @Header(name = "Location", description = "Created Admin Series URL"),
                            @Header(name = "ETag", description = "Strong decimal Series revision")
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
                    description = "Series slug conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Series validation failed")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SeriesAdminDetail> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SeriesEditable.class)))
            @RequestBody(required = false) JsonNode body) {
        SeriesEditable editable = SeriesEditable.parse(body);
        SeriesView created = creator.create(editable.toContentInput());
        URI location = URI.create(BASE_PATH + "/" + created.record().id());
        return ResponseEntity.created(location)
                .eTag(RevisionHttpHeaders.etag(created.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(SeriesAdminDetail.from(created));
    }

    @Operation(
            operationId = "getAdminSeries",
            summary = "Get a Series for Admin editing")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Series returned with its Modules in order",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Strong decimal Series revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Series id is not a valid UUID"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Series was not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SeriesAdminDetail> get(@PathVariable UUID id) {
        SeriesView series = reader.get(id);
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(series.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(SeriesAdminDetail.from(series));
    }

    @Operation(
            operationId = "updateSeries",
            summary = "Replace the editable state of a Series")
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
                    description = "Series updated",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal Series revision")),
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
                    description = "Series was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Series slug conflicts or is immutable"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Series validation failed"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SeriesAdminDetail> update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SeriesEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        SeriesEditable editable = SeriesEditable.parse(body);
        SeriesView updated = editor.replace(id, expectedRevision, editable.toContentInput());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(updated.record().revision()))
                .cacheControl(CacheControl.noStore())
                .body(SeriesAdminDetail.from(updated));
    }

    @Operation(
            operationId = "reorderModules",
            summary = "Replace the order of all Modules of a Series")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision of the Series, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order saved, or already as requested",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "New strong decimal Series revision")),
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
                    description = "Series was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "The ids are not exactly the current Modules of the Series"),
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
            value = "/{seriesId}/module-order",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ModuleOrderResponse> reorderModules(
            @PathVariable UUID seriesId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ModuleOrderEditable.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request) {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        ModuleOrderEditable editable = ModuleOrderEditable.parse(body);
        ChildOrderView order = orderEditor.reorderModules(
                seriesId, expectedRevision, editable.moduleIds());
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(order.parentRevision()))
                .cacheControl(CacheControl.noStore())
                .body(ModuleOrderResponse.from(order));
    }
}
