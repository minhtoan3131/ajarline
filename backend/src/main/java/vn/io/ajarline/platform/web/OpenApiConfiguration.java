package vn.io.ajarline.platform.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

        return new OpenAPI()
                .info(new Info()
                        .title("Ajarline API")
                        .version("1")
                        .description("Source-generated documentation for implemented Ajarline HTTP APIs."))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_SESSION, adminSession));
    }
}
