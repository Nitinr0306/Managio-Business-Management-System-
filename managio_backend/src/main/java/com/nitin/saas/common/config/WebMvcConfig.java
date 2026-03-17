package com.nitin.saas.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * FIX B11: The previous implementation registered a second, conflicting CORS
 * config via WebMvcConfigurer that:
 *   (a) used allowedOriginPatterns("*") with allowCredentials(true) — browsers
 *       reject wildcard origin combined with credentials per the CORS spec.
 *   (b) overlapped with the authoritative CORS config already registered in
 *       SecurityConfig.corsConfigurationSource(), causing undefined precedence.
 *
 * The single source of truth for CORS is SecurityConfig.corsConfigurationSource()
 * which is applied by the Spring Security filter chain before MVC processing.
 * This class is intentionally left empty — the @Configuration annotation keeps
 * it in the component scan so it can be extended later if non-API CORS rules
 * (e.g., for static resources) are ever needed.
 */
@Configuration
public class WebMvcConfig {
    // CORS is configured exclusively in SecurityConfig.corsConfigurationSource().
    // Do not add a second addCorsMappings() here.
}