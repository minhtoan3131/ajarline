package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import vn.io.ajarline.platform.web.ApiProblem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.io.ajarline.platform.security.authentication.AdminLoginLockedException;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@RestControllerAdvice
public class AuthExceptionHandler {

    private final ApiProblemWriter problems;

    public AuthExceptionHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(AdminLoginLockedException.class)
    ResponseEntity<ApiProblem> locked(
            AdminLoginLockedException exception, HttpServletRequest request) {
        ResponseEntity<ApiProblem> problem = problems.response(request, 429,
                "ADMIN_LOGIN_LOCKED", "Admin login locked",
                "Try again after the lock window expires.");
        return ResponseEntity.status(problem.getStatusCode())
                .headers(problem.getHeaders())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(problem.getBody());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiProblem> invalidCredentials(HttpServletRequest request) {
        return problems.response(request, 401, "INVALID_CREDENTIALS",
                "Authentication failed", "Authentication failed.");
    }

    @ExceptionHandler(AuthController.InvalidAuthRequestException.class)
    ResponseEntity<ApiProblem> invalidRequest(HttpServletRequest request) {
        return problems.response(request, 400, "INVALID_REQUEST",
                "Request is invalid", "Request is invalid.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiProblem> malformedJson(HttpServletRequest request) {
        return problems.response(request, 400, "MALFORMED_JSON",
                "Request JSON is malformed", "Request JSON is malformed.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiProblem> unsupportedMediaType(HttpServletRequest request) {
        return problems.response(request, 415, "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported media type", "Use application/json.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(HttpServletRequest request) {
        return problems.response(request, 500, "INTERNAL_ERROR",
                "Unexpected internal failure", "Unexpected internal failure.");
    }
}
