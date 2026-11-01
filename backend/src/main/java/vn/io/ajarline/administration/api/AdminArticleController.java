package vn.io.ajarline.administration.api;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleEditor;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleView;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("api/admin/v1/articles")
public class AdminArticleController {
    private static final String BASE_PATH = "/api/admin/v1/articles";
    private final ArticleCreator creator;
    private final ArticleReader reader;

    public AdminArticleController(ArticleCreator creator, ArticleReader reader) {
        this.creator = creator;
        this.reader = reader;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ArticleAdminDetail> create(@RequestBody(required = false) JsonNode body) {
        ArticleEditable editable = ArticleEditable.parse(body);
        //TODO: Coordinate module membership and topic assignment through their owners.
        ArticleView created = creator.create(editable.toContentInput());
        URI location = URI.create(BASE_PATH + "/" + created.id());
        return ResponseEntity.created(location)
                .eTag(RevisionHttpHeaders.etag(created.revision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleAdminDetail.from(created));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArticleAdminDetail> get(@PathVariable UUID id) {
        ArticleView article = reader.get(id);
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(article.revision()))
                .cacheControl(CacheControl.noStore())
                .body(ArticleAdminDetail.from(article));

    }
}
