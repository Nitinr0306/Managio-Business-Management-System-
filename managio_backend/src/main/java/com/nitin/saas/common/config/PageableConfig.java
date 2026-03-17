package com.nitin.saas.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * P6 FIX: Without a max-page-size cap any authenticated client can request
 * Integer.MAX_VALUE rows in a single page — effectively a full table dump.
 * This customizer caps requests at 200 rows and sets a sensible default of 20.
 */
@Configuration
public class PageableConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(200);          // hard ceiling — server ignores larger values
            resolver.setFallbackPageable(          // default when caller omits ?page / ?size
                    org.springframework.data.domain.PageRequest.of(0, 20));
            resolver.setOneIndexedParameters(false);
        };
    }
}