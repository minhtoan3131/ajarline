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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.platform.web.OpenApiConfiguration;
import vn.io.ajarline.topic.contract.TopicCreator;
import vn.io.ajarline.topic.contract.TopicEditor;
import vn.io.ajarline.topic.contract.TopicReader;
import vn.io.ajarline.topic.contract.TopicView;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Admin Topic", description = "Authenticated Topic catalog operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("api/admin/v1/topics")
public class AdminTopicController {
    private static final String BASE_PATH = "/api/admin/v1/topics";
    private final TopicCreator creator;
    private final TopicReader reader;
    private final TopicEditor editor;

    public AdminTopicController(TopicCreator creator, TopicReader reader, TopicEditor editor) {
        this.creator = creator;
        this.reader = reader;
        this.editor = editor;
    }

    @Operation(
            operationId = "listAdminTopics",
            summary = "List every Topic, active or not")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Topics returned, ordered by name and then by id",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TopicAdminList> list() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TopicAdminList.from(reader.list()));
    }

    @Operation(
            operationId = "createTopic",
            summary = "Create a Topic")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Topic created",
                    useReturnTypeSchema = true,
                    headers = @Header(name = "Location", description = "Created Admin Topic URL")),
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
                    description = "Topic slug conflicts"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Topic validation failed")
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TopicAdminItem> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicCreatable.class)))
            @RequestBody(required = false) JsonNode body) {
        TopicCreatable creatable = TopicCreatable.parse(body);
        TopicView created = creator.create(creatable.toInput());
        URI location = URI.create(BASE_PATH + "/" + created.id());
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(TopicAdminItem.from(created));
    }

    @Operation(
            operationId = "updateTopic",
            summary = "Replace the editable state of a Topic")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Topic updated, or returned unchanged when no value differs",
                    useReturnTypeSchema = true),
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
                    responseCode = "404",
                    description = "Topic was not found"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Topic validation failed")
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TopicAdminItem> update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TopicEditable.class)))
            @RequestBody(required = false) JsonNode body) {
        TopicEditable editable = TopicEditable.parse(body);
        TopicView updated = editor.replace(id, editable.toInput());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TopicAdminItem.from(updated));
    }
}
