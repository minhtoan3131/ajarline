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

@Tag(name = "Public Discovery", description = "Anonymous browsing of public content.")
@RestController
@RequestMapping("/api/v1/articles")
public class PublicArticleCollectionController {

    private final PublicContentService content;

    public PublicArticleCollectionController(PublicContentService content) {
        this.content = content;
    }

    @Operation(
            operationId = "listStandaloneArticles",
            summary = "List public standalone Articles")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Standalone public Articles, ordered by title and then by id",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Standalone, page or size query is invalid")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ArticleCollectionResponse list(
            @Parameter(
                    description = "Required and must be true. There is no public list of every Article.",
                    required = true,
                    schema = @Schema(type = "boolean", allowableValues = "true"))
            @RequestParam(required = false) String standalone,
            @Parameter(
                    description = "Zero-based page number.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(
                    description = "Number of Articles per page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        // The query shape is checked first, so a bad query never reaches the database.
        ArticleCollectionQuery query = ArticleCollectionQuery.parse(standalone, page, size);
        return ArticleCollectionResponse.from(query, content.findStandaloneArticles(query.page(), query.size()));
    }
}
