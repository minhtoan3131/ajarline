package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import vn.io.ajarline.platform.security.AdminAuthenticationProvider;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.MutableClock;

/**
 * Shared HTTP fixture for Admin authentication boundary tests.
 */
abstract class AbstractAdminAuthenticationIT {

    protected static final String AUTH = "/api/auth/v1";
    protected static final String ADMIN_PROBE = "/api/admin/v1/security-probe";
    protected static final MutableClock LOGIN_CLOCK = new MutableClock(
            Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);
    protected static final ObjectMapper JSON = JsonMapper.builder().build();

    private final List<HttpClient> clients = new ArrayList<>();

    @LocalServerPort
    protected int port;

    @Autowired
    protected ServletContext servletContext;

    @Autowired
    private ProbeController probe;

    @TestBean(name = "adminAuthenticationProvider",
            methodName = "controlledAuthenticationProvider", enforceOverride = true)
    private AdminAuthenticationProvider provider;

    static AdminAuthenticationProvider controlledAuthenticationProvider() {
        return new AdminAuthenticationProvider(
                AdminAuthenticationTestData.credentials(),
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(), LOGIN_CLOCK);
    }

    @BeforeEach
    void prepareAccountAndProbe() {
        LOGIN_CLOCK.set(LOGIN_CLOCK.instant().plus(Duration.ofDays(1)));
        provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password()));
        probe.mutations.set(0);
    }

    @AfterEach
    void closeHttpClients() {
        clients.forEach(HttpClient::close);
    }

    protected int mutationCount() {
        return probe.mutations.get();
    }

    protected Browser authenticatedBrowser() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> response = login(browser, csrf(browser),
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());
        assertJson(response, 200, "{\"authenticated\":true}");
        return browser;
    }

    protected HttpClient httpClient() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        clients.add(client);
        return client;
    }

    protected String csrf(Browser browser) throws Exception {
        HttpResponse<String> response = browser.get(AUTH + "/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("token", "headerName");
        assertThat(body.get("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        assertThat(body.get("token").isTextual()).isTrue();
        assertThat(body.get("token").asText().isBlank()).isFalse();
        return body.get("token").asText();
    }

    protected HttpResponse<String> login(
            Browser browser, String token, String username, String password) throws Exception {
        String body = JSON.writeValueAsString(Map.of(
                "username", username,
                "password", password));
        return browser.post(AUTH + "/login", body, token);
    }

    protected static void assertJson(
            HttpResponse<String> response, int status, String expected) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertNoStore(response);
        assertThat(JSON.readTree(response.body())).isEqualTo(JSON.readTree(expected));
    }

    protected static void assertNoStore(HttpResponse<?> response) {
        assertThat(response.headers().firstValue("Cache-Control").orElse(""))
                .contains("no-store");
    }

    protected static void assertProblem(
            HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "type", "title", "status", "detail", "code", "requestId");
        assertThat(body.get("type").asText()).isEqualTo("about:blank");
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("title").asText().isBlank()).isFalse();
        assertThat(body.get("detail").asText().isBlank()).isFalse();
        assertThat(body.get("requestId").asText().isBlank()).isFalse();
    }

    /**
     * Replays the session cookie explicitly on loopback HTTP. Cookie attributes
     * are tested separately; this helper does not model browser HTTPS rules.
     */
    protected final class Browser {

        private final HttpClient client = httpClient();
        private String cookie;

        protected Browser() {
        }

        protected Browser(String cookie) {
            this.cookie = cookie;
        }

        protected String cookie() {
            return cookie;
        }

        protected HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, null, null);
        }

        protected HttpResponse<String> post(
                String path, String body, String token) throws Exception {
            return send("POST", path, body, token,
                    body == null ? null : "application/json");
        }

        protected HttpResponse<String> send(
                String method, String path, String body, String token, String contentType)
                throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(10));
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            if (token != null) {
                request.header("X-CSRF-TOKEN", token);
            }
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            request.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = client.send(
                    request.build(), HttpResponse.BodyHandlers.ofString());
            for (String value : response.headers().allValues("Set-Cookie")) {
                if (value.startsWith("AJARLINE_SESSION=")) {
                    cookie = value.split(";", 2)[0];
                }
            }
            return response;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    @TestComponent
    static class ProbeController {

        private final AtomicInteger mutations = new AtomicInteger();

        @GetMapping({"/api/v1/security-probe", "/api/admin/v1/security-probe"})
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void read() {
        }

        @PostMapping("/api/admin/v1/security-probe")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void mutate() {
            mutations.incrementAndGet();
        }
    }
}
