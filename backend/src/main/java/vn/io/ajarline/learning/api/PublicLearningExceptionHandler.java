package vn.io.ajarline.learning.api;

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
        PublicSeriesCollectionController.class,
        PublicSeriesController.class,
        PublicModuleController.class
})
public class PublicLearningExceptionHandler {

    private final ApiProblemWriter problems;

    public PublicLearningExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(PublicLearningNotFoundException.class)
    ResponseEntity<ApiProblem> notFound(
            PublicLearningNotFoundException exception, HttpServletRequest request) {
        String title = exception.kind().label() + " is not publicly available";
        return problems.response(request, 404, "CONTENT_NOT_FOUND", title, title + ".");
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
    ResponseEntity<ApiProblem> unexpected(HttpServletRequest request) {
        return problems.response(
                request,
                500,
                "INTERNAL_ERROR",
                "Unexpected internal failure",
                "Unexpected internal failure.");
    }
}
