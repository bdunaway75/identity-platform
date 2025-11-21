package io.github.blakedunaway.authserver.config

import jakarta.validation.Validator
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@TestConfiguration
@EnableTransactionManagement
class TestConfig {

    @Bean
    Validator validator() {
        new LocalValidatorFactoryBean()
    }
}
