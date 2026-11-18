package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminSeriesController.class)
public class AdminLearningExceptionHandler {

    private final ApiProblemWriter problems;

    public AdminLearningExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(AdminApiException.class)
    ResponseEntity<ApiProblem> boundary(AdminApiException exception, HttpServletRequest request) {
        return problems.response(
                request,
                exception.status(),
                exception.code(),
                exception.title(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(ContentOperationException.class)
    ResponseEntity<ApiProblem> content(ContentOperationException exception, HttpServletRequest request) {
        return AdminFailureMapping.response(problems, request, exception.code().name());
    }

    @ExceptionHandler(LearningOperationException.class)
    ResponseEntity<ApiProblem> learning(LearningOperationException exception, HttpServletRequest request) {
        return AdminFailureMapping.response(problems, request, exception.code().name());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiProblem> malformedJson(HttpServletRequest request) {
        return problems.response(
                request, 400, "MALFORMED_JSON",
                "Request JSON is malformed", "Request JSON is malformed.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiProblem> unsupportedMediaType(HttpServletRequest request) {
        return problems.response(
                request, 415, "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported media type", "Use application/json.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiProblem> invalidUuid(HttpServletRequest request) {
        return problems.response(
                request, 400, "INVALID_UUID",
                "UUID is invalid", "id must be a valid UUID.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(HttpServletRequest request) {
        return problems.response(
                request, 500, "INTERNAL_ERROR",
                "Unexpected internal failure", "Unexpected internal failure.");
    }
}
