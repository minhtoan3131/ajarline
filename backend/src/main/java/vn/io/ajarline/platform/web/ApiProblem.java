package vn.io.ajarline.platform.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        requiredProperties = {"type", "title", "status", "detail", "code", "requestId"}
)
public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String requestId
) {
}
