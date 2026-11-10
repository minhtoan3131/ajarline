package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;
import vn.io.ajarline.publication.contract.PublicationOperationException;


@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminPublicationController.class)
public class AdminPublicationExceptionHandler {

    private final ApiProblemWriter problems;

    public AdminPublicationExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(AdminApiException.class)
    ResponseEntity<ApiProblem> boundary(
            AdminApiException failure,
            HttpServletRequest request
    ) {
        return problems.response(
                request,
                failure.status(),
                failure.code(),
                failure.title(),
                failure.getMessage());
    }

    @ExceptionHandler(PublicationOperationException.class)
    ResponseEntity<ApiProblem> publication(
            PublicationOperationException failure,
            HttpServletRequest request
    ) {
        return switch (failure.code()) {
            case CONTENT_SUMMARY_REQUIRED -> problems.response(
                    request, 422, failure.code().name(),
                    "Content cannot be published because required content or hierarchy validation failed",
                    "Content cannot be published because required content or hierarchy validation failed.");
            case ARTICLE_BODY_REQUIRED -> problems.response(
                    request, 422, failure.code().name(),
                    "Content cannot be published because required content or hierarchy validation failed",
                    "Content cannot be published because required content or hierarchy validation failed.");
            case INVALID_PUBLICATION_STATE -> problems.response(
                    request, 409, failure.code().name(),
                    "Content cannot be unpublished from the current state",
                    "Content cannot be unpublished from the current state.");
        };
    }

    @ExceptionHandler(ContentOperationException.class)
    ResponseEntity<ApiProblem> content(
            ContentOperationException failure,
            HttpServletRequest request
    ) {
        if (failure.code() == ContentFailureCode.CONTENT_NOT_FOUND) {
            return problems.response(
                    request, 404, "CONTENT_NOT_FOUND",
                    "Content was not found", "Content was not found.");
        }
        if (failure.code() == ContentFailureCode.REVISION_CONFLICT) {
            return problems.response(
                    request, 412, "REVISION_CONFLICT",
                    "Revision precondition failed",
                    "Revision precondition failed.");
        }
        return internal(request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiProblem> invalidUuid(
            HttpServletRequest request
    ) {
        return problems.response(
                request, 400, "INVALID_UUID",
                "UUID is invalid", "id must be a valid UUID.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(
            HttpServletRequest request
    ) {
        return internal(request);
    }

    private ResponseEntity<ApiProblem> internal(
            HttpServletRequest request
    ) {
        return problems.response(
                request, 500, "INTERNAL_ERROR",
                "Unexpected internal failure",
                "Unexpected internal failure.");
    }
}
