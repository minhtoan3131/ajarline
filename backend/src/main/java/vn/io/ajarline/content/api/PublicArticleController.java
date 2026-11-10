package vn.io.ajarline.content.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.content.contract.ArticleReader;

@Tag(name = "Public Content", description = "Anonymous public content reads.")
@RestController
@RequestMapping("/api/v1/articles")
public class PublicArticleController {

    private final ArticleReader articles;

    public PublicArticleController(ArticleReader articles) {
        this.articles = articles;
    }

    @Operation(
            operationId = "getPublicArticle",
            summary = "Get a public Article by slug")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public Article returned",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "404",
                    description = "Article is not publicly available")
    })
    @GetMapping(
            value = "/{slug}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PublicArticleResponse get(@PathVariable String slug) {
        return articles.findPublishedBySlug(slug)
                .map(PublicArticleResponse::from)
                .orElseThrow(PublicArticleNotFoundException::new);
    }
}
