package vn.io.ajarline.platform.web;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    public static final String ADMIN_SESSION = "adminSession";

    @Bean
    OpenAPI ajarlineOpenApi() {
        SecurityScheme adminSession = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("AJARLINE_SESSION")
                .description("HttpOnly Admin session created by the login operation.");

        Components components = new Components()
                .addSecuritySchemes(ADMIN_SESSION, adminSession);
        ModelConverters.getInstance()
                .read(ApiProblem.class)
                .forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("Ajarline API")
                        .version("1")
                        .description("Source-generated documentation for implemented Ajarline HTTP APIs."))
                .components(components);
    }

    @Bean
    OperationCustomizer apiResponsePolicyDocumentation() {
        return (operation, ignored) -> {
            operation.getResponses().putIfAbsent(
                    "500",
                    internalServerErrorResponse());
            operation.getResponses().forEach((status, response) -> {
                boolean errorResponse = status.startsWith("4") || status.startsWith("5");
                boolean missingProblemContent = response.getContent() == null
                        || !response.getContent().containsKey(
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                if (errorResponse && missingProblemContent) {
                    response.content(problemContent());
                }
                response.addHeaderObject(
                        "Cache-Control",
                        noStoreHeader());
            });
            return operation;
        };
    }

    private ApiResponse internalServerErrorResponse() {
        return new ApiResponse()
                .description("Unexpected internal failure (INTERNAL_ERROR)")
                .content(problemContent());
    }

    private Content problemContent() {
        return new Content().addMediaType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>()
                                .$ref("#/components/schemas/ApiProblem")));
    }

    private Header noStoreHeader() {
        return new Header()
                .description("The response must not be stored by caches")
                .schema(new StringSchema().addEnumItem("no-store"));
    }
}
