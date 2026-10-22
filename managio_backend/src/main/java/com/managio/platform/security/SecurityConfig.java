package com.managio.platform.security;

import com.managio.platform.config.ManagioProperties;
import com.managio.platform.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    /** Endpoints that work without a token; bearer tokens sent to them are ignored. */
    private static final String[] PUBLIC_API = {
            "/api/v1/auth/**", "/api/v1/portal/auth/**", "/api/v1/public/**"};

    @Bean
    SecurityFilterChain api(HttpSecurity http, AccessTokens accessTokens, JsonMapper jsonMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // bearer tokens; the refresh cookie is SameSite=Lax (D-009)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // On top of Spring Security's defaults (nosniff, frame DENY, no-store): API responses are JSON, so
                // nothing in them may load, run or be framed, no referrer leaves with them, and nothing is indexed.
                .headers(headers -> headers
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                request -> request.getRequestURI().startsWith("/api/"),
                                new StaticHeadersWriter("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")))
                        // API responses are private data; search engines must not index them even if linked.
                        .addHeaderWriter(new StaticHeadersWriter("X-Robots-Tag", "noindex, nofollow")))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(PUBLIC_API).permitAll()
                        // Invitation links are opened before the invitee has an account. The token travels in
                        // the body, never the URL, so it stays out of logs. A signed-in invitee's token is honoured.
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/preview", "/api/v1/invitations/accept")
                        .permitAll()
                        .requestMatchers("/api/v1/portal/**").hasAuthority(PrincipalAuthentication.ROLE_CUSTOMER)
                        .requestMatchers("/api/v1/**").hasAuthority(PrincipalAuthentication.ROLE_USER)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.decoder(accessTokens.decoder())
                                .jwtAuthenticationConverter(new PrincipalAuthentication.FromJwt()))
                        .authenticationEntryPoint((request, response, ex) -> writeProblem(jsonMapper, response,
                                Problems.of(HttpStatus.UNAUTHORIZED, "auth.required", "Sign in required",
                                        "Your session has ended. Sign in to continue.")))
                        .accessDeniedHandler((request, response, ex) -> writeForbidden(jsonMapper, response)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeProblem(jsonMapper, response,
                                Problems.of(HttpStatus.UNAUTHORIZED, "auth.required", "Sign in required",
                                        "Sign in to continue.")))
                        .accessDeniedHandler((request, response, e) -> writeForbidden(jsonMapper, response)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(AccessTokens accessTokens) {
        return accessTokens.decoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ManagioProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.security().corsAllowedOrigins() == null
                ? List.of() : properties.security().corsAllowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "Idempotency-Key"));
        cors.setExposedHeaders(List.of("X-Request-Id"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return (HttpServletRequest request) -> isPublic(request.getRequestURI()) ? null : delegate.resolve(request);
    }

    private static boolean isPublic(String uri) {
        return uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/portal/auth/") || uri.startsWith("/api/v1/public/");
    }

    private static void writeForbidden(JsonMapper jsonMapper, HttpServletResponse response) throws IOException {
        writeProblem(jsonMapper, response, Problems.of(HttpStatus.FORBIDDEN, "auth.forbidden", "Not allowed",
                "You don't have permission to do this."));
    }

    private static void writeProblem(JsonMapper jsonMapper, HttpServletResponse response, ProblemDetail problem)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        if (problem.getProperties() != null) {
            body.putAll(problem.getProperties());
        }
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
