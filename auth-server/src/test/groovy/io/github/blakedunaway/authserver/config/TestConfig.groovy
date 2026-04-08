package io.github.blakedunaway.authserver.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.blakedunaway.authserver.config.redis.RedisStore
import jakarta.persistence.EntityManagerFactory
import jakarta.validation.Validator
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(basePackages = "io.github.blakedunaway.authserver.integration.repository.jpa")
@ComponentScan(["io.github.blakedunaway.authserver.integration.repository", "io.github.blakedunaway.authserver.mapper"])
@EntityScan(basePackages = "io.github.blakedunaway.authserver.integration.entity")
@EnableTransactionManagement
class TestConfig {

    @Primary
    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
                                            .build();
    }

    @Primary
    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,
                32,
                1,
                1 << 16,
                2
        );
    }

    @Primary
    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("io.github.blakedunaway.authserver.integration.entity");

        HibernateJpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
        vendor.setGenerateDdl(true);
        factoryBean.setJpaVendorAdapter(vendor);

        Properties jpaProps = new Properties();

        jpaProps.put("hibernate.hbm2ddl.auto", "create-drop");
        factoryBean.setJpaProperties(jpaProps);
        return factoryBean;
    }

    @Primary
    @Bean
    JpaTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }

    @Bean
    Validator validator() {
        new LocalValidatorFactoryBean()
    }

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return om;
    }
}
