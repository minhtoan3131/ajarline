package vn.io.ajarline.topic.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PublicTopicController.class)
public class PublicTopicExceptionHandler {

    private final ApiProblemWriter problems;

    public PublicTopicExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(PublicTopicNotFoundException.class)
    ResponseEntity<ApiProblem> notFound(HttpServletRequest request) {
        return problems.response(
                request,
                404,
                "CONTENT_NOT_FOUND",
                "Topic is not publicly available",
                "Topic is not publicly available.");
    }

    @ExceptionHandler(PublicTopicQueryException.class)
    ResponseEntity<ApiProblem> invalidQuery(
            PublicTopicQueryException exception, HttpServletRequest request) {
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
