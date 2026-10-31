package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminAuthenticationFailureIT extends AbstractAdminAuthenticationIT {

    @Test
    void login_ShouldReturnGenericFailure_WhenEitherCredentialIsWrong() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);

        HttpResponse<String> wrongUsername = login(browser, token,
                "unknown-admin", AdminAuthenticationTestData.password());
        HttpResponse<String> wrongPassword = login(browser, token,
                AdminAuthenticationTestData.username(), "invalid");

        assertProblem(wrongUsername, 401, "INVALID_CREDENTIALS");
        assertProblem(wrongPassword, 401, "INVALID_CREDENTIALS");
        JsonNode first = JSON.readTree(wrongUsername.body());
        JsonNode second = JSON.readTree(wrongPassword.body());
        assertThat(first.get("title")).isEqualTo(second.get("title"));
        assertThat(first.get("detail")).isEqualTo(second.get("detail"));
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":false}");
    }

    @Test
    void login_ShouldRejectBody_WhenJsonOrFieldsAreInvalid() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);
        String valid = JSON.writeValueAsString(Map.of(
                "username", AdminAuthenticationTestData.username(),
                "password", AdminAuthenticationTestData.password()));
        String unknownField = valid.substring(0, valid.length() - 1) + ",\"extra\":true}";

        assertProblem(browser.post(AUTH + "/login", "{", token),
                400, "MALFORMED_JSON");
        for (String body : new String[]{
                "{}", "[]", "null", "{\"username\":null,\"password\":\"x\"}",
                "{\"username\":\" \",\"password\":\"x\"}",
                "{\"username\":\"admin\",\"password\":1}",
                "{\"username\":\"admin\",\"password\":\"\"}",
                JSON.writeValueAsString(Map.of(
                        "username", "a".repeat(201), "password", "x")),
                JSON.writeValueAsString(Map.of(
                        "username", "admin", "password", "a".repeat(1025))),
                unknownField}) {
            assertProblem(browser.post(AUTH + "/login", body, token),
                    400, "INVALID_REQUEST");
        }
        assertProblem(browser.send(
                        "POST", AUTH + "/login", valid, token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":false}");
    }

    @Test
    void logout_ShouldKeepSession_WhenBodyWasSupplied() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(AUTH + "/logout", " ", token);

        assertProblem(response, 400, "INVALID_REQUEST");
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":true}");
    }

    @Test
    void logout_ShouldRequireAuthentication_WhenOnlyAnonymousCsrfExists() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(AUTH + "/logout", null, token);

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void login_ShouldCreateRequestId_WhenClientSuppliesCorrelationHeader() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);
        HttpClient client = httpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + AUTH + "/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", browser.cookie())
                .header("X-CSRF-TOKEN", token)
                .header("X-Request-ID", "untrusted-correlation")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{"))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertProblem(response, 400, "MALFORMED_JSON");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("requestId").asText())
                .isNotEqualTo("untrusted-correlation");
        assertThat(response.body())
                .doesNotContain(
                        AdminAuthenticationTestData.password(),
                        AdminAuthenticationTestData.passwordHash());
    }
}
