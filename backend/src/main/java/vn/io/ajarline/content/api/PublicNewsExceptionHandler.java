package vn.io.ajarline.content.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;
import vn.io.ajarline.platform.web.ApiQueryException;
import vn.io.ajarline.platform.web.ApiRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        PublicNewsController.class,
        PublicNewsCollectionController.class
})
public class PublicNewsExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PublicNewsExceptionHandler.class);

    private final ApiProblemWriter problems;

    public PublicNewsExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(PublicNewsNotFoundException.class)
    ResponseEntity<ApiProblem> notFound(HttpServletRequest request) {
        return problems.response(
                request,
                404,
                "CONTENT_NOT_FOUND",
                "News is not publicly available",
                "News is not publicly available.");
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
    ResponseEntity<ApiProblem> unexpected(Exception exception, HttpServletRequest request) {
        // A published News item with broken data ends here. The log is the only way to learn
        // about it, so it must not be skipped.
        LOG.error("Public News request failed. requestId={}",
                request.getAttribute(ApiRequestFilter.REQUEST_ID_ATTRIBUTE), exception);
        return problems.response(
                request,
                500,
                "INTERNAL_ERROR",
                "Unexpected internal failure",
                "Unexpected internal failure.");
    }
}
