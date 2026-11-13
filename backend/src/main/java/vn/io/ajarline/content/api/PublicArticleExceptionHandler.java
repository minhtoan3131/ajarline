package vn.io.ajarline.content.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@RestControllerAdvice(assignableTypes = PublicArticleController.class)
public class PublicArticleExceptionHandler {

    private final ApiProblemWriter problems;

    public PublicArticleExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(PublicArticleNotFoundException.class)
    ResponseEntity<ApiProblem> notFound(
            HttpServletRequest request
    ) {
        return problems.response(
                request,
                404,
                "CONTENT_NOT_FOUND",
                "Article is not publicly available",
                "Article is not publicly available.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(
            HttpServletRequest request
    ) {
        return problems.response(
                request,
                500,
                "INTERNAL_ERROR",
                "Unexpected internal failure",
                "Unexpected internal failure.");
    }
}
