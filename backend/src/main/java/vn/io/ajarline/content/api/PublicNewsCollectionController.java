package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.content.application.PublicContentService;
import vn.io.ajarline.platform.web.PageQuery;

@Tag(name = "Public Discovery", description = "Anonymous browsing of public content.")
@RestController
@RequestMapping("/api/v1/news")
public class PublicNewsCollectionController {

    private final PublicContentService content;

    public PublicNewsCollectionController(PublicContentService content) {
        this.content = content;
    }

    @Operation(
            operationId = "listPublicNews",
            summary = "List public News")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public News ordered by publishedAt DESC, id ASC",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Page or size query is invalid")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public NewsCollectionResponse list(
            @Parameter(
                    description = "Zero-based page number.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(
                    description = "Number of News items per page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        // The query is checked first, so a bad query never reaches the database.
        PageQuery query = PageQuery.parse(page, size);
        return NewsCollectionResponse.from(query, content.findPublicNews(query.page(), query.size()));
    }
}
