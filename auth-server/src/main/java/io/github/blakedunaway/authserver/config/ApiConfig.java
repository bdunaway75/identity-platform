package io.github.blakedunaway.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.github.blakedunaway.authserver.util.ApiService;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.provider.JavaTimeTypesParamConverterProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml"})
public class ApiConfig {

    @Autowired
    private Bus bus;

    @Autowired
    private BuildProperties buildProperties;

    @Bean
    @Autowired
    public Server rsServer(final List<ApiService> services) {
        JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setServiceBeans(new ArrayList<>(services));
        endpoint.setAddress("/");
        endpoint.setProviders(
                Arrays.asList(
                        new JavaTimeTypesParamConverterProvider(),
                        new JacksonJsonProvider(new ObjectMapper().registerModule(new JavaTimeModule())
                                                                  .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false))
                )
        );
        endpoint.setFeatures(Arrays.asList(swagger2Feature(), loggingFeature()));
        return endpoint.create();
    }

    private Swagger2Feature swagger2Feature() {
        Swagger2Feature swagger2Feature = new Swagger2Feature();
        swagger2Feature.setPrettyPrint(true);
        swagger2Feature.setTitle(buildProperties.getName());
        swagger2Feature.setDescription("Auth-Service");
        swagger2Feature.setVersion(buildProperties.getVersion());
        swagger2Feature.setUsePathBasedConfig(true);
        swagger2Feature.setSwaggerUiConfig(new SwaggerUiConfig().url("swagger.json").queryConfigEnabled(false));
        return swagger2Feature;
    }

    private LoggingFeature loggingFeature() {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        return loggingFeature;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ServletRegistrationBean cxfServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new CXFServlet(), "/rest/*");
        registration.setLoadOnStartup(-1);
        return registration;
    }

}
