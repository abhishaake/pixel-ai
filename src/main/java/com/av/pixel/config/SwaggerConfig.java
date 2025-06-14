package com.av.pixel.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .packagesToScan("com.av.pixel.controller")
                .pathsToExclude("/api/v1/utility")
                .pathsToMatch("/api/test/**")
                .group("rest-api")
                .build();
    }
}
