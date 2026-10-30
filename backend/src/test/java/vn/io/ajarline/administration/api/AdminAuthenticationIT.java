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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class, AdminAuthenticationIT.ProbeConfiguration.class})
class AdminAuthenticationIT {

    private static final String AUTH = "/api/auth/v1";
    private static final String ADMIN_PROBE = "/api/admin/v1/security-probe";
    private static final MutableClock LOGIN_CLOCK = new MutableClock(
            Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private final List<HttpClient> clients = new ArrayList<>();

    @LocalServerPort
    private int port;

    @Autowired
    private ProbeController probe;

    @Autowired
    private ServletContext servletContext;

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
                AdminAuthenticationTestData.username(), AdminAuthenticationTestData.password()));
        probe.mutations.set(0);
    }

    @AfterEach
    void closeHttpClients() {
        clients.forEach(HttpClient::close);
    }

    @Test
    void session_ShouldReportAnonymousState_WhenNoSessionExists() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = browser.get(AUTH + "/session");

        assertJson(response, 200, "{\"authenticated\":false}");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    void access_ShouldRequireSession_WhenRouteIsAdmin() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> publicResponse = browser.get("/api/v1/security-probe");
        HttpResponse<String> adminResponse = browser.get(ADMIN_PROBE);

        assertThat(publicResponse.statusCode()).isEqualTo(204);
        assertProblem(adminResponse, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void login_ShouldRotateSessionAndReplaceCsrf_WhenCredentialsAreValid() throws Exception {
        Browser browser = new Browser();
        String oldToken = csrf(browser);
        String oldCookie = browser.cookie;

        HttpResponse<String> response = login(browser, oldToken,
                AdminAuthenticationTestData.username(), AdminAuthenticationTestData.password());

        assertJson(response, 200, "{\"authenticated\":true}");
        assertThat(!oldCookie.equals(browser.cookie)).as("session ID changed").isTrue();
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":true}");
        assertThat(browser.get(ADMIN_PROBE).statusCode()).isEqualTo(204);

        Browser oldSession = new Browser(oldCookie);
        assertProblem(oldSession.get(ADMIN_PROBE), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(browser.post(ADMIN_PROBE, null, oldToken), 403, "CSRF_INVALID");
        assertProblem(browser.post(AUTH + "/logout", null, oldToken), 403, "CSRF_INVALID");
        assertThat(probe.mutations.get()).isZero();

        String freshToken = csrf(browser);
        assertThat(browser.post(ADMIN_PROBE, null, freshToken).statusCode()).isEqualTo(204);
        assertThat(probe.mutations.get()).isEqualTo(1);
    }

    @Test
    void logout_ShouldInvalidateSession_WhenCurrentCsrfIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String authenticatedCookie = browser.cookie;

        HttpResponse<String> response = browser.post(AUTH + "/logout", null, token);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertNoStore(response);
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":false}");

        Browser replay = new Browser(authenticatedCookie);
        assertProblem(replay.get(ADMIN_PROBE), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(replay.post(ADMIN_PROBE, null, token), 403, "CSRF_INVALID");
        assertThat(probe.mutations.get()).isZero();
    }

    private Browser authenticatedBrowser() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> response = login(browser, csrf(browser),
                AdminAuthenticationTestData.username(), AdminAuthenticationTestData.password());
        assertJson(response, 200, "{\"authenticated\":true}");
        return browser;
    }

    private HttpClient httpClient() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        clients.add(client);
        return client;
    }

    private String csrf(Browser browser) throws Exception {
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

    private HttpResponse<String> login(
            Browser browser, String token, String username, String password) throws Exception {
        String body = JSON.writeValueAsString(Map.of("username", username, "password", password));
        return browser.post(AUTH + "/login", body, token);
    }

    private static void assertJson(HttpResponse<String> response, int status, String expected) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertNoStore(response);
        assertThat(JSON.readTree(response.body())).isEqualTo(JSON.readTree(expected));
    }

    private static void assertNoStore(HttpResponse<?> response) {
        assertThat(response.headers().firstValue("Cache-Control").orElse(""))
                .contains("no-store");
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
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
    private final class Browser {

        private final HttpClient client = httpClient();
        private String cookie;

        private Browser() {
        }

        private Browser(String cookie) {
            this.cookie = cookie;
        }

        private HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, null, null);
        }

        private HttpResponse<String> post(String path, String body, String token) throws Exception {
            return send("POST", path, body, token, body == null ? null : "application/json");
        }

        private HttpResponse<String> send(
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
