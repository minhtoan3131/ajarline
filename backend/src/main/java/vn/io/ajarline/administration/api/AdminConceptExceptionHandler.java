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
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminConceptController.class)
public class AdminConceptExceptionHandler {

    private final ApiProblemWriter problems;

    public AdminConceptExceptionHandler(ApiProblemWriter problems) {
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

    @ExceptionHandler(ConceptOperationException.class)
    ResponseEntity<ApiProblem> concept(ConceptOperationException exception, HttpServletRequest request) {
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

    private ProblemMapping mapping(ConceptFailureCode code) {
        return switch (code) {
            case VALIDATION_FAILED -> new ProblemMapping(422, "Request validation failed",
                    "One or more fields are invalid.");
            case DUPLICATE_CONCEPT_EXPRESSION -> new ProblemMapping(422, "Concept expressions are duplicated",
                    "Two expressions are the same after normalization. Keep each expression once.");
            case CONCEPT_KEY_CONFLICT -> new ProblemMapping(409, "Concept key conflicts with an existing Concept",
                    "A Concept with this key already exists.");
        };
    }
}
