package vn.io.ajarline.platform.web;

public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String requestId
) {
}
