package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.io.ajarline.platform.web.OpenApiConfiguration;
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.ContentUnpublisher;
import vn.io.ajarline.publication.contract.PublicationResult;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "Admin Publication", description = "Authenticated content publication operations.")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
@RestController
@RequestMapping("/api/admin/v1/content")
public class AdminPublicationController {
    private final ContentPublisher publisher;
    private final ContentUnpublisher unpublisher;

    public AdminPublicationController(ContentPublisher publisher, ContentUnpublisher unpublisher) {
        this.publisher = publisher;
        this.unpublisher = unpublisher;
    }


    @Operation(
            operationId = "publishContent",
            summary = "Publish or republish Content")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision returned in ETag, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Content published or returned unchanged when already published",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Current strong decimal content revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request, UUID or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Content was not found"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Publication validation failed"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PostMapping(
            value = "/{id}/publish",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublicationStateResponse> publish(
            @PathVariable UUID id,
            @Parameter(hidden = true) HttpServletRequest request
    ) throws IOException {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        requireEmptyBody(request);
        PublicationResult result = publisher.publish(id, expectedRevision);
        return response(result);
    }


    @Operation(
            operationId = "unpublishContent",
            summary = "Unpublish Content without cascading to descendants")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @Parameter(
            name = "If-Match",
            in = ParameterIn.HEADER,
            required = true,
            description = "Strong decimal revision returned in ETag, for example \"3\".")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Content unpublished or returned unchanged when already unpublished",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "ETag",
                            description = "Current strong decimal content revision")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request, UUID or If-Match syntax is invalid"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Content was not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Content cannot be unpublished from its current state"),
            @ApiResponse(
                    responseCode = "412",
                    description = "If-Match revision is stale"),
            @ApiResponse(
                    responseCode = "428",
                    description = "If-Match is required")
    })
    @PostMapping(
            value = "/{id}/unpublish",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublicationStateResponse> unpublish(
            @PathVariable UUID id,
            @Parameter(hidden = true) HttpServletRequest request
    ) throws IOException {
        long expectedRevision = RevisionHttpHeaders.requiredExpectedRevision(request);
        requireEmptyBody(request);
        PublicationResult result = unpublisher.unpublish(id, expectedRevision);
        return response(result);
    }

    private ResponseEntity<PublicationStateResponse> response(PublicationResult result) {
        return ResponseEntity.ok()
                .eTag(RevisionHttpHeaders.etag(result.revision()))
                .cacheControl(CacheControl.noStore())
                .body(PublicationStateResponse.from(result));
    }
    private void requireEmptyBody(HttpServletRequest request) throws IOException {
        if (request.getInputStream().read() != -1) {
            throw AdminApiException.invalidRequest("Request body must be empty.");
        }
    }
}
