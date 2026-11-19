package vn.io.ajarline.learning.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.learning.application.PublicLearningService;

@Tag(name = "Public Content", description = "Anonymous public content reads.")
@RestController
@RequestMapping("/api/v1/modules")
public class PublicModuleController {

    private final PublicLearningService learning;

    public PublicModuleController(PublicLearningService learning) {
        this.learning = learning;
    }

    @Operation(
            operationId = "getPublicModule",
            summary = "Get a public Module by slug")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public Module returned",
                    useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "404",
                    description = "Module is not publicly available")
    })
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PublicModuleResponse get(@PathVariable String slug) {
        return learning.findPublicModule(slug)
                .map(PublicModuleResponse::from)
                .orElseThrow(() -> new PublicLearningNotFoundException(
                        PublicLearningNotFoundException.Kind.MODULE));
    }
}
