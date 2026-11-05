package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
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
    ResponseEntity<Map<String, Object>> locked(
            AdminLoginLockedException exception, HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> problem = problems.response(request, 429,
                "ADMIN_LOGIN_LOCKED", "Admin login locked",
                "Try again after the lock window expires.");
        return ResponseEntity.status(problem.getStatusCode())
                .headers(problem.getHeaders())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(problem.getBody());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, Object>> invalidCredentials(HttpServletRequest request) {
        return problems.response(request, 401, "INVALID_CREDENTIALS",
                "Authentication failed", "Authentication failed.");
    }

    @ExceptionHandler(AuthController.InvalidAuthRequestException.class)
    ResponseEntity<Map<String, Object>> invalidRequest(HttpServletRequest request) {
        return problems.response(request, 400, "INVALID_REQUEST",
                "Request is invalid", "Request is invalid.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> malformedJson(HttpServletRequest request) {
        return problems.response(request, 400, "MALFORMED_JSON",
                "Request JSON is malformed", "Request JSON is malformed.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, Object>> unsupportedMediaType(HttpServletRequest request) {
        return problems.response(request, 415, "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported media type", "Use application/json.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(HttpServletRequest request) {
        return problems.response(request, 500, "INTERNAL_ERROR",
                "Unexpected internal failure", "Unexpected internal failure.");
    }
}
