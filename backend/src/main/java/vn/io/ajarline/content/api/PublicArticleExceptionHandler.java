package vn.io.ajarline.content.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;
import vn.io.ajarline.platform.web.ApiQueryException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        PublicArticleController.class,
        PublicArticleCollectionController.class
})
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

    @ExceptionHandler(ApiQueryException.class)
    ResponseEntity<ApiProblem> invalidQuery(
            ApiQueryException exception, HttpServletRequest request) {
        return problems.response(
                request,
                400,
                exception.code(),
                exception.title(),
                exception.getMessage());
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
