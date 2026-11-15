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
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminArticleController.class)
public class AdminArticleExceptionHandler {

    private final ApiProblemWriter problems;

    public AdminArticleExceptionHandler(ApiProblemWriter problems) {
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
        ProblemMapping mapping = mapping(exception.code());
        return problems.response(
                request,
                mapping.status,
                exception.code().name(),
                mapping.title,
                mapping.detail
        );
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

    private record ProblemMapping(int status, String title, String detail) {

    }

    private ProblemMapping mapping(ContentFailureCode code) {
        return switch (code) {
            case CONTENT_NOT_FOUND -> new ProblemMapping(404, "Article was not found",
                    "Article was not found.");
            case VALIDATION_FAILED -> new ProblemMapping(422, "Request validation failed",
                    "One or more fields are invalid.");
            case SLUG_CONFLICT -> new ProblemMapping(409, "Article slug conflicts",
                    "An article with this slug already exists.");
            case SLUG_IMMUTABLE_AFTER_PUBLICATION -> new ProblemMapping(409, "Article slug cannot be changed",
                    "The slug cannot change after first publication.");
            case REVISION_CONFLICT ->
                    new ProblemMapping(412, "Revision conflict", "The resource changed since it was loaded.");
            case TOPIC_REFERENCE_NOT_FOUND -> new ProblemMapping(422, "Topic reference was not found",
                    "One or more Topic ids do not exist.");
            case INACTIVE_TOPIC_ASSIGNMENT -> new ProblemMapping(422, "Topic cannot be assigned",
                    "An inactive Topic cannot be newly assigned.");
        };
    }
}
