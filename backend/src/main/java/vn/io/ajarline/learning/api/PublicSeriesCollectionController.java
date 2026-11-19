package vn.io.ajarline.learning.api;

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
import vn.io.ajarline.learning.application.PublicLearningService;
import vn.io.ajarline.platform.web.PageQuery;

@Tag(name = "Public Discovery", description = "Anonymous browsing of public content.")
@RestController
@RequestMapping("/api/v1/series")
public class PublicSeriesCollectionController {

    private final PublicLearningService learning;

    public PublicSeriesCollectionController(PublicLearningService learning) {
        this.learning = learning;
    }

    @Operation(
            operationId = "listPublicSeries",
            summary = "List public Series")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public Series, ordered by title and then by id",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Page or size query is invalid")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public SeriesCollectionResponse list(
            @Parameter(
                    description = "Zero-based page number.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(
                    description = "Number of Series per page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        // The query shape is checked first, so a bad query never reaches the database.
        PageQuery query = PageQuery.parse(page, size);
        return SeriesCollectionResponse.from(query, learning.findPublicSeriesPage(query.page(), query.size()));
    }
}
