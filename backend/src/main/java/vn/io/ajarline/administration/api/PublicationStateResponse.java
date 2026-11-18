package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.PublicationResult;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
@Schema(requiredProperties = {
        "id", "type", "status", "revision", "publishedAt", "unpublishedAt", "updatedAt"
})
public record PublicationStateResponse(
        UUID id,
        @Schema(allowableValues = {"article", "module", "series"}) String type,
        PublicationStatus status,
        long revision,
        @Schema(nullable = true) Instant publishedAt,
        @Schema(nullable = true) Instant unpublishedAt,
        Instant updatedAt
        ) {
    static PublicationStateResponse from(PublicationResult result) {
        return new PublicationStateResponse(
                result.id(),
                result.type().name().toLowerCase(Locale.ROOT),
                result.status(),
                result.revision(),
                result.publishedAt(),
                result.unpublishedAt(),
                result.updatedAt()
        );
    }

}
