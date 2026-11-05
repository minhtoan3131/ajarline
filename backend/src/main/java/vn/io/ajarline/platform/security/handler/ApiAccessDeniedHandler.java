package vn.io.ajarline.platform.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import vn.io.ajarline.platform.web.ApiProblemWriter;

public final class ApiAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ApiProblemWriter problems;

    public ApiAccessDeniedHandler(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        if (exception instanceof CsrfException) {
            problems.write(
                    request,
                    response,
                    403,
                    "CSRF_INVALID",
                    "CSRF proof is missing or invalid",
                    "CSRF proof is missing or invalid.");
            return;
        }

        problems.write(
                request,
                response,
                500,
                "INTERNAL_ERROR",
                "Unexpected internal failure",
                "Unexpected internal failure.");
    }
}
