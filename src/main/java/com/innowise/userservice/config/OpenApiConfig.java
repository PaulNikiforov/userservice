package com.innowise.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * <p>Configures the OpenAPI specification for the UserService API,
 * including server URLs, API information, and contact details.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI bean for API documentation.
     *
     * @return the configured OpenAPI specification
     */
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Service API")
                .description("REST API for managing users and payment cards")
                .version("1.0.0")
                )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development")
            ));
    }
}
