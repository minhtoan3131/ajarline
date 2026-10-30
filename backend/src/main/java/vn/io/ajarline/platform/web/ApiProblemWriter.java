package vn.io.ajarline.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class ApiProblemWriter {
    private final ObjectMapper mapper;

    public ApiProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ResponseEntity<Map<String, Object>> response(
            HttpServletRequest request, int status, String code, String title, String detail
    ) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .body(body(request, status, code, title, detail));
    }

    public void write(
            HttpServletRequest request, HttpServletResponse response,
            int status, String code, String title, String detail
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), body(request, status, code, title, detail));
    }

    private Map<String, Object> body(
            HttpServletRequest request, int status, String code, String title, String detail
    ) {
        return Map.of(
                "type", "about:blank",
                "title", title,
                "status", status,
                "detail", detail,
                "code", code,
                "requestId", request.getAttribute(ApiRequestFilter.REQUEST_ID_ATTRIBUTE)
        );
    }
}
