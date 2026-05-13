package com.swd301.foodmarket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nông Sản Xấu Mã – Food Market API")
                        .version("1.0.0")
                        .description(
                                "API documentation for the Nông Sản Xấu Mã (Food Market) project. " +
                                        "This Spring Boot REST API supports agricultural product management, " +
                                        "user authentication, order processing, and role-based access control."
                        )
                        .contact(new Contact()
                                .name("Nông Sản Xấu Mã Development Team")
                                .email("support@foodmarket.com")
                                .url("https://foodmarket.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
                .addSecurityItem(
                        new SecurityRequirement().addList("Bearer Authentication")
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT Bearer token to authorize")
                        )
                );
    }
}
