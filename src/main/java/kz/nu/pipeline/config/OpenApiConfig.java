package kz.nu.pipeline.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Field surveys of culverts",
                version = "1.0.0",
                description = "not the worst team",
                termsOfService = "termOfService",
                contact = @Contact(
                        name = "Nurzhan",
                        email = "nurzhan.kozhamuratov@alumni.nu.edu.kz"
                ),
                license = @License(
                        name = "license"
                )
        ),
        security = {
                @SecurityRequirement(name = "auth")
        }
)
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("auth", new SecurityScheme()
                                .name("auth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }
}
