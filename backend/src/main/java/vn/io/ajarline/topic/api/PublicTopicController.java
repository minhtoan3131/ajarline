package vn.io.ajarline.topic.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.topic.application.PublicTopicContent;
import vn.io.ajarline.topic.application.PublicTopicService;

@Tag(name = "Public Discovery", description = "Anonymous browsing by Topic.")
@RestController
@RequestMapping("/api/v1/topics")
public class PublicTopicController {

    private final PublicTopicService topics;

    public PublicTopicController(PublicTopicService topics) {
        this.topics = topics;
    }

    @Operation(
            operationId = "listPublicTopics",
            summary = "List the Topics that have public content")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public Topics, ordered by name and then by id",
                    useReturnTypeSchema = true)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public TopicDiscoveryResponse list() {
        return TopicDiscoveryResponse.from(topics.listPublicTopics());
    }

    @Operation(
            operationId = "getPublicTopic",
            summary = "Get a public Topic by slug")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public Topic returned",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "404",
                    description = "Topic is not publicly available")
    })
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PublicTopicResponse get(@PathVariable String slug) {
        return topics.findPublicTopic(slug)
                .map(PublicTopicResponse::from)
                .orElseThrow(PublicTopicNotFoundException::new);
    }

    @Operation(
            operationId = "listPublicTopicContent",
            summary = "List the public content directly assigned to a Topic")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "One page of public content, ordered by type and then by title",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Type, page or size query is invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Topic is not publicly available")
    })
    @GetMapping(value = "/{slug}/content", produces = MediaType.APPLICATION_JSON_VALUE)
    public TopicContentResponse content(
            @PathVariable String slug,
            @Parameter(
                    description = "Comma-separated content types. Absent means every type.",
                    style = ParameterStyle.FORM,
                    explode = Explode.FALSE,
                    array = @ArraySchema(
                            uniqueItems = true,
                            schema = @Schema(allowableValues = {"series", "module", "article", "news"})))
            @RequestParam(required = false) String types,
            @Parameter(
                    description = "Zero-based page number.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(
                    description = "Number of items per page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        // The query shape is checked before the Topic is looked up, so a bad
        // query gets 400 even when the Topic does not exist.
        TopicContentQuery query = TopicContentQuery.parse(types, page, size);
        PublicTopicContent content = topics
                .findPublicContent(slug, query.contentTypes(), query.page(), query.size())
                .orElseThrow(PublicTopicNotFoundException::new);
        return TopicContentResponse.from(query, content);
    }
}
