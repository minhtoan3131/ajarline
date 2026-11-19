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
import vn.io.ajarline.content.contract.NewsReader;

@Tag(name = "Public Content", description = "Anonymous public content reads.")
@RestController
@RequestMapping("/api/v1/news")
public class PublicNewsController {

    private final NewsReader news;

    public PublicNewsController(NewsReader news) {
        this.news = news;
    }

    @Operation(
            operationId = "getPublicNews",
            summary = "Get public News by slug")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public News with external source metadata",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "404",
                    description = "News is not publicly available")
    })
    @GetMapping(
            value = "/{slug}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PublicNewsResponse get(@PathVariable String slug) {
        return news.findPublishedBySlug(slug)
                .map(PublicNewsResponse::from)
                .orElseThrow(PublicNewsNotFoundException::new);
    }
}
