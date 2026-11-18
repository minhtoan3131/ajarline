package vn.io.ajarline.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSqlTestConfiguration.class)
class ApiDocumentationIT {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private static final Map<String, Map<String, String>> OPERATIONS = Map.ofEntries(
            Map.entry("/api/auth/v1/csrf", Map.of("get", "getCsrfToken")),
            Map.entry("/api/auth/v1/session", Map.of("get", "getSessionState")),
            Map.entry("/api/auth/v1/login", Map.of("post", "loginAdmin")),
            Map.entry("/api/auth/v1/logout", Map.of("post", "logoutAdmin")),
            Map.entry("/api/admin/v1/articles", Map.of("post", "createArticle")),
            Map.entry("/api/admin/v1/articles/{id}", Map.of(
                    "get", "getAdminArticle",
                    "put", "updateArticle")),
            Map.entry("/api/admin/v1/series", Map.of("post", "createSeries")),
            Map.entry("/api/admin/v1/series/{id}", Map.of(
                    "get", "getAdminSeries",
                    "put", "updateSeries")),
            Map.entry("/api/admin/v1/modules", Map.of("post", "createModule")),
            Map.entry("/api/admin/v1/modules/{id}", Map.of(
                    "get", "getAdminModule",
                    "put", "updateModule")),
            Map.entry("/api/admin/v1/topics", Map.of(
                    "get", "listAdminTopics",
                    "post", "createTopic")),
            Map.entry("/api/admin/v1/topics/{id}", Map.of(
                    "put", "updateTopic",
                    "delete", "deleteTopic")),
            Map.entry("/api/admin/v1/content/{id}/publish", Map.of(
                    "post", "publishContent")),
            Map.entry("/api/admin/v1/content/{id}/unpublish", Map.of(
                    "post", "unpublishContent")),
            Map.entry("/api/v1/articles", Map.of(
                    "get", "listStandaloneArticles")),
            Map.entry("/api/v1/articles/{slug}", Map.of(
                    "get", "getPublicArticle")),
            Map.entry("/api/v1/topics", Map.of(
                    "get", "listPublicTopics")),
            Map.entry("/api/v1/topics/{slug}", Map.of(
                    "get", "getPublicTopic")),
            Map.entry("/api/v1/topics/{slug}/content", Map.of(
                    "get", "listPublicTopicContent")));

    @LocalServerPort
    private int port;

    @Test
    void openApi_ShouldListImplementedOperations_WhenDocumentIsGenerated()
            throws Exception {
        
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        
        Set<String> actualPaths = new HashSet<>(paths.propertyNames());

        assertThat(actualPaths).containsExactlyInAnyOrderElementsOf(OPERATIONS.keySet());
        OPERATIONS.forEach((path, methods) -> {
            assertThat(paths.get(path).propertyNames())
                    .containsExactlyInAnyOrderElementsOf(methods.keySet());
            methods.forEach((method, operationId) ->
                    assertThat(operation(paths, path, method)
                            .get("operationId").asString()).isEqualTo(operationId));
        });
    }

    @Test
    void openApi_ShouldExposeCrossCuttingResponses_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode paths = openApiDocument().get("paths");

        OPERATIONS.forEach((path, methods) -> methods.keySet().forEach(method -> {
            JsonNode documentedOperation = operation(paths, path, method);
            JsonNode responses = documentedOperation.get("responses");
            boolean adminOrAuth = path.startsWith("/api/admin/")
                    || path.startsWith("/api/auth/");

            assertThat(problemSchemaRef(documentedOperation, "500"))
                    .endsWith("/ApiProblem");
            responses.properties().forEach(response -> {
                boolean errorResponse = response.getKey().startsWith("4")
                        || response.getKey().startsWith("5");
                if (errorResponse) {
                    assertThat(problemSchemaRef(
                            documentedOperation, response.getKey()))
                            .endsWith("/ApiProblem");
                }
                if (adminOrAuth || errorResponse) {
                    assertThat(hasResponseHeader(
                            response.getValue(), "Cache-Control")).isTrue();
                }
            });
        }));

        JsonNode publicSuccess = operation(
                paths, "/api/v1/articles/{slug}", "get")
                .get("responses").get("200");
        assertThat(hasResponseHeader(publicSuccess, "Cache-Control")).isFalse();
    }

    @Test
    void openApi_ShouldExposeAuthMetadata_WhenDocumentIsGenerated()
            throws Exception {
        
        JsonNode document = openApiDocument();
        JsonNode csrf = operation(document.get("paths"),
                "/api/auth/v1/csrf", "get");
        JsonNode login = operation(document.get("paths"),
                "/api/auth/v1/login", "post");
        JsonNode logout = operation(document.get("paths"),
                "/api/auth/v1/logout", "post");
        
        JsonNode loginSchema = document.get("components")
                .get("schemas").get("AdminLoginRequest");

        assertThat(csrf.get("responses").get("200")
                .get("headers").has("Set-Cookie")).isTrue();
        assertThat(parameterNames(login)).contains("X-CSRF-TOKEN");
        assertThat(requestSchemaRef(login))
                .endsWith("/AdminLoginRequest");
        assertThat(login.get("responses").get("200")
                .get("headers").has("Set-Cookie")).isTrue();
        assertThat(login.get("responses").get("429")
                .get("headers").has("Retry-After")).isTrue();
        assertThat(problemSchemaRef(login, "429"))
                .endsWith("/ApiProblem");
        assertThat(loginSchema.get("properties").get("password")
                .get("writeOnly").asBoolean()).isTrue();
        assertThat(hasSecurity(logout, OpenApiConfiguration.ADMIN_SESSION)).isTrue();
    }

    @Test
    void openApi_ShouldExposeSeriesMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode create = operation(paths, "/api/admin/v1/series", "post");
        JsonNode get = operation(paths, "/api/admin/v1/series/{id}", "get");
        JsonNode update = operation(paths, "/api/admin/v1/series/{id}", "put");

        for (JsonNode seriesOperation : new JsonNode[]{create, get, update}) {
            assertThat(hasSecurity(seriesOperation, OpenApiConfiguration.ADMIN_SESSION)).isTrue();
        }
        assertThat(parameterNames(create)).contains("X-CSRF-TOKEN");
        assertThat(parameterNames(update)).contains("X-CSRF-TOKEN", "If-Match");
        assertThat(requestSchemaRef(create)).endsWith("/SeriesEditable");
        assertThat(requestSchemaRef(update)).endsWith("/SeriesEditable");
        assertThat(responseSchemaRef(get, "200")).endsWith("/SeriesAdminDetail");
        assertThat(create.get("responses").get("201")
                .get("headers").propertyNames())
                .contains("Location", "ETag");
        assertThat(get.get("responses").get("200")
                .get("headers").has("ETag")).isTrue();
        assertThat(update.get("responses").get("200")
                .get("headers").has("ETag")).isTrue();
        assertThat(get.get("responses").has("404")).isTrue();
        assertThat(update.get("responses").propertyNames())
                .contains("404", "409", "412", "422", "428");

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.propertyNames()).contains(
                "SeriesEditable", "SeriesAdminDetail", "AdminChildItem");
        assertThat(schemas.get("SeriesAdminDetail").get("properties").has("modules")).isTrue();
    }

    @Test
    void openApi_ShouldExposeArticleMetadata_WhenDocumentIsGenerated()
            throws Exception {
        
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode create = operation(paths, "/api/admin/v1/articles", "post");
        JsonNode update = operation(paths, "/api/admin/v1/articles/{id}", "put");

        Set<String> updateParameters = parameterNames(update);
        
        assertThat(hasSecurity(create, OpenApiConfiguration.ADMIN_SESSION)).isTrue();
        assertThat(parameterNames(create)).contains("X-CSRF-TOKEN");
        assertThat(updateParameters).contains("X-CSRF-TOKEN", "If-Match");
        assertThat(requestSchemaRef(create)).endsWith("/ArticleEditable");
        assertThat(requestSchemaRef(update)).endsWith("/ArticleEditable");
        assertThat(create.get("responses").get("201")
                .get("headers").propertyNames())
                .contains("Location", "ETag");
        assertThat(update.get("responses").get("200")
                .get("headers").has("ETag")).isTrue();

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.propertyNames()).contains(
                "ArticleEditable", "ArticleAdminDetail", "ApiProblem");
    }

    @Test
    void openApi_ShouldExposePublicationMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode publish = operation(paths,
                "/api/admin/v1/content/{id}/publish", "post");
        JsonNode unpublish = operation(paths,
                "/api/admin/v1/content/{id}/unpublish", "post");

        for (JsonNode publication : new JsonNode[]{publish, unpublish}) {
            assertThat(hasSecurity(
                    publication,
                    OpenApiConfiguration.ADMIN_SESSION)).isTrue();
            assertThat(parameterNames(publication))
                    .contains("X-CSRF-TOKEN", "If-Match");
            assertThat(publication.get("responses").get("200")
                    .get("headers").has("ETag")).isTrue();
            assertThat(responseSchemaRef(publication, "200"))
                    .endsWith("/PublicationStateResponse");
        }

        assertThat(publish.get("responses").has("422")).isTrue();
        assertThat(unpublish.get("responses").has("409")).isTrue();
    }

    @Test
    void swaggerUi_ShouldReturnPage_WhenRequestedWithoutAuthentication()
            throws Exception {
        
        HttpRequest request = get("/swagger-ui/index.html");

        HttpResponse<String> response = send(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("text/html");
        assertThat(response.body()).containsIgnoringCase("swagger ui");
    }

    @Test
    void openApi_ShouldExposePublicArticleMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode publicArticle = operation(
                document.get("paths"),
                "/api/v1/articles/{slug}",
                "get");

        assertThat(hasSecurity(
                publicArticle,
                OpenApiConfiguration.ADMIN_SESSION)).isFalse();
        assertThat(responseSchemaRef(publicArticle, "200"))
                .endsWith("/PublicArticleResponse");
        assertThat(problemSchemaRef(publicArticle, "404"))
                .endsWith("/ApiProblem");

        JsonNode properties = document.get("components")
                .get("schemas")
                .get("PublicArticleResponse")
                .get("properties");
        assertThat(properties.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "bodyMarkdown",
                "topics", "series", "module", "previousArticle",
                "nextArticle", "publishedAt", "updatedAt");
        assertThat(properties.has("status")).isFalse();
        assertThat(properties.has("revision")).isFalse();
        assertThat(properties.has("unpublishedAt")).isFalse();
    }

    @Test
    void openApi_ShouldExposeTopicMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode list = operation(paths, "/api/admin/v1/topics", "get");
        JsonNode create = operation(paths, "/api/admin/v1/topics", "post");
        JsonNode update = operation(paths, "/api/admin/v1/topics/{id}", "put");
        JsonNode delete = operation(paths, "/api/admin/v1/topics/{id}", "delete");

        for (JsonNode topicOperation : new JsonNode[]{list, create, update, delete}) {
            assertThat(hasSecurity(topicOperation, OpenApiConfiguration.ADMIN_SESSION)).isTrue();
        }
        for (JsonNode mutation : new JsonNode[]{create, update, delete}) {
            assertThat(parameterNames(mutation)).contains("X-CSRF-TOKEN");
        }
        assertThat(parameterNames(update)).doesNotContain("If-Match");
        assertThat(requestSchemaRef(create)).endsWith("/TopicCreatable");
        assertThat(requestSchemaRef(update)).endsWith("/TopicEditable");
        assertThat(responseSchemaRef(list, "200")).endsWith("/TopicAdminList");
        assertThat(create.get("responses").get("201").get("headers").propertyNames())
                .contains("Location");
        assertThat(create.get("responses").get("201").get("headers").has("ETag")).isFalse();
        assertThat(delete.get("responses").has("204")).isTrue();
        assertThat(delete.get("responses").has("409")).isTrue();

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.propertyNames()).contains(
                "TopicCreatable", "TopicEditable", "TopicAdminItem", "TopicAdminList");
        assertThat(schemas.get("TopicEditable").get("properties").has("slug")).isFalse();
    }

    @Test
    void openApi_ShouldExposePublicTopicMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode list = operation(paths, "/api/v1/topics", "get");
        JsonNode get = operation(paths, "/api/v1/topics/{slug}", "get");
        JsonNode content = operation(paths, "/api/v1/topics/{slug}/content", "get");

        for (JsonNode publicOperation : new JsonNode[]{list, get, content}) {
            assertThat(hasSecurity(publicOperation, OpenApiConfiguration.ADMIN_SESSION)).isFalse();
        }
        assertThat(responseSchemaRef(list, "200")).endsWith("/TopicDiscoveryResponse");
        assertThat(responseSchemaRef(get, "200")).endsWith("/PublicTopicResponse");
        assertThat(responseSchemaRef(content, "200")).endsWith("/TopicContentResponse");
        assertThat(get.get("responses").has("404")).isTrue();
        assertThat(content.get("responses").has("400")).isTrue();
        assertThat(content.get("responses").has("404")).isTrue();
        assertThat(parameterNames(content)).contains("slug", "types", "page", "size");

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.get("PublicTopicResponse").get("properties").propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name", "description");
    }

    @Test
    void openApi_ShouldExposeStandaloneArticleMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode list = operation(document.get("paths"), "/api/v1/articles", "get");

        assertThat(hasSecurity(list, OpenApiConfiguration.ADMIN_SESSION)).isFalse();
        assertThat(list.get("tags")).hasSize(1);
        assertThat(list.get("tags").get(0).asString()).isEqualTo("Public Discovery");
        assertThat(parameterNames(list)).containsExactlyInAnyOrder("standalone", "page", "size");
        assertThat(requiredParameters(list)).containsExactly("standalone");
        assertThat(responseSchemaRef(list, "200")).endsWith("/ArticleCollectionResponse");
        assertThat(list.get("responses").has("400")).isTrue();
        assertThat(list.get("responses").has("404")).isFalse();

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.get("ArticleCollectionResponse").get("properties").propertyNames())
                .containsExactlyInAnyOrder("page", "size", "hasMore", "items");
        assertThat(schemas.get("ArticleSummary").get("properties").propertyNames())
                .containsExactlyInAnyOrder(
                        "id", "type", "slug", "title", "summary",
                        "topics", "publishedAt", "updatedAt");
    }

    @Test
    void openApi_ShouldExposeModuleMetadata_WhenDocumentIsGenerated()
            throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.get("paths");
        JsonNode create = operation(paths, "/api/admin/v1/modules", "post");
        JsonNode get = operation(paths, "/api/admin/v1/modules/{id}", "get");
        JsonNode update = operation(paths, "/api/admin/v1/modules/{id}", "put");

        for (JsonNode moduleOperation : new JsonNode[]{create, get, update}) {
            assertThat(hasSecurity(moduleOperation, OpenApiConfiguration.ADMIN_SESSION)).isTrue();
        }
        assertThat(parameterNames(create)).contains("X-CSRF-TOKEN");
        assertThat(parameterNames(update)).contains("X-CSRF-TOKEN", "If-Match");
        assertThat(requestSchemaRef(create)).endsWith("/ModuleEditable");
        assertThat(requestSchemaRef(update)).endsWith("/ModuleEditable");
        assertThat(responseSchemaRef(get, "200")).endsWith("/ModuleAdminDetail");
        assertThat(create.get("responses").get("201")
                .get("headers").propertyNames())
                .contains("Location", "ETag");
        assertThat(update.get("responses").get("200")
                .get("headers").has("ETag")).isTrue();
        assertThat(update.get("responses").propertyNames())
                .contains("404", "409", "412", "422", "428");

        JsonNode schemas = document.get("components").get("schemas");
        assertThat(schemas.propertyNames()).contains("ModuleEditable", "ModuleAdminDetail");
        assertThat(schemas.get("ModuleAdminDetail").get("properties").propertyNames())
                .contains("seriesId", "position", "articles");
    }

    private JsonNode openApiDocument() throws Exception {
        HttpResponse<String> response = send(get("/v3/api-docs"));
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static JsonNode operation(
            JsonNode paths, String path, String method) {
        return paths.get(path).get(method);
    }

    private static Set<String> parameterNames(JsonNode operation) {
        Set<String> names = new HashSet<>();
        JsonNode parameters = operation.get("parameters");
        if (parameters != null) {
            for (JsonNode parameter : parameters) {
                names.add(parameter.get("name").asString());
            }
        }
        return names;
    }

    private static Set<String> requiredParameters(JsonNode operation) {
        Set<String> names = new HashSet<>();
        for (JsonNode parameter : operation.get("parameters")) {
            if (parameter.has("required") && parameter.get("required").asBoolean()) {
                names.add(parameter.get("name").asString());
            }
        }
        return names;
    }

    private static boolean hasSecurity(JsonNode operation, String scheme) {
        JsonNode requirements = operation.get("security");
        if (requirements == null) {
            return false;
        }
        for (JsonNode requirement : requirements) {
            if (requirement.has(scheme)) {
                return true;
            }
        }
        return false;
    }

    private static String requestSchemaRef(JsonNode operation) {
        return operation.get("requestBody")
                .get("content")
                .get("application/json")
                .get("schema")
                .get("$ref")
                .asString();
    }

    private static String responseSchemaRef(
            JsonNode operation, String responseCode) {
        return operation.get("responses")
                .get(responseCode)
                .get("content")
                .get("application/json")
                .get("schema")
                .get("$ref")
                .asString();
    }

    private static String problemSchemaRef(
            JsonNode operation, String responseCode) {
        return operation.get("responses")
                .get(responseCode)
                .get("content")
                .get("application/problem+json")
                .get("schema")
                .get("$ref")
                .asString();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static boolean hasResponseHeader(
            JsonNode response,
            String headerName
    ) {
        JsonNode headers = response.get("headers");
        return headers != null && headers.has(headerName);
    }
}
