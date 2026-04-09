package io.github.blakedunaway.authserver.config.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.github.blakedunaway.authserver")
@EnableJpaRepositories(basePackages = "io.github.blakedunaway.authserver.integration.repository")
@EntityScan(basePackages = "io.github.blakedunaway.authserver.integration.entity")
public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernateJsonCustomizer() {
        final ObjectMapper hibernateObjectMapper = new ObjectMapper();
        hibernateObjectMapper.registerModule(new JavaTimeModule());
        hibernateObjectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        hibernateObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return props ->
                props.put("hibernate.type.json_format_mapper", new JacksonJsonFormatMapper(hibernateObjectMapper));
    }

}

