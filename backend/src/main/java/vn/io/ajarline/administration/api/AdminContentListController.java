package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.content.contract.ContentIndexPage;
import vn.io.ajarline.content.contract.ContentIndexReader;
import vn.io.ajarline.platform.web.OpenApiConfiguration;

@Tag(name = "Admin Content", description = "Authenticated content authoring operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("/api/admin/v1/content")
public class AdminContentListController {
    private final ContentIndexReader reader;

    public AdminContentListController(ContentIndexReader reader) {
        this.reader = reader;
    }

    @Operation(
            operationId = "listAdminContent",
            summary = "List and filter content of every status")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "One page, ordered by update time (newest first) and then by id",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "A query value is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminContentList> list(
            @Parameter(
                    description = "Comma-separated types: series, module, article, news. No value means every type.",
                    schema = @Schema(type = "string"))
            @RequestParam(required = false) String types,
            @Parameter(
                    description = "Comma-separated statuses: DRAFT, PUBLISHED, UNPUBLISHED. No value means every status.",
                    schema = @Schema(type = "string"))
            @RequestParam(required = false) String statuses,
            @Parameter(
                    description = "Literal, case-insensitive text searched in the title and the slug.",
                    schema = @Schema(type = "string", minLength = 1, maxLength = 300))
            @RequestParam(required = false) String q,
            @Parameter(
                    description = "Zero-based page number.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(
                    description = "Number of items per page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        // The query shape is checked first, so a bad query never reaches the database.
        AdminContentQuery query = AdminContentQuery.parse(types, statuses, q, page, size);
        ContentIndexPage result = reader.find(query.toFilter(), query.page(), query.size());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AdminContentList.from(query, result));
    }
}
