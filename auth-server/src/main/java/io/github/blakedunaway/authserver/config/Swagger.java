package io.github.blakedunaway.authserver.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Auth Service", version = "v1",
                description = "Client registration & OAuth flows")
)
@Configuration
public class Swagger {
}
