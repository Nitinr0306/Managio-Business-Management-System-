package com.nitin.saas.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthFilter               jwtAuthFilter;
        private final JwtAuthenticationEntryPoint authenticationEntryPoint;
        private final ObjectMapper                objectMapper;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(jsonAccessDeniedHandler()))
                        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                        .authorizeHttpRequests(auth -> auth

                                // ── User auth (public) ───────────────────────────────────────────
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",
                                        "/api/v1/auth/verify-email",
                                        "/api/v1/auth/resend-verification-email",
                                        "/api/v1/auth/staff/login"
                                ).permitAll()

                                // ── Member auth (public) ─────────────────────────────────────────
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/members/auth/login",
                                        "/api/v1/members/auth/register",
                                        "/api/v1/members/auth/forgot-password",
                                        "/api/v1/members/auth/reset-password",     // FIX: was missing
                                        "/api/v1/members/auth/verify-email"
                                ).permitAll()

                                // ── Staff invitation (public — token-gated by the service) ────────
                                .requestMatchers(HttpMethod.POST,  "/api/v1/staff/accept-invitation").permitAll()
                                .requestMatchers(HttpMethod.GET,   "/api/v1/staff/invitation").permitAll()

                                // ── Infrastructure ───────────────────────────────────────────────
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/actuator/health",
                                        "/api/health",
                                        "/api/info"
                                ).permitAll()

                                .anyRequest().authenticated()
                        );

                return http.build();
        }

        @Bean
        public AccessDeniedHandler jsonAccessDeniedHandler() {
                return (request, response, ex) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        Map<String, Object> body = new HashMap<>();
                        body.put("timestamp", LocalDateTime.now().toString());
                        body.put("status",  403);
                        body.put("error",   "Forbidden");
                        body.put("message", "Access denied");
                        body.put("errorCode", "AUTH_002");
                        body.put("path",    request.getServletPath());
                        objectMapper.writeValue(response.getOutputStream(), body);
                };
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
                throws Exception {
                return cfg.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "https://*.vercel.app",
                        "https://yourdomain.com"));
                config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
                config.setAllowedHeaders(List.of(
                        "Authorization","Content-Type",
                        "X-Refresh-Token","X-Device-Id","X-Request-Id"));
                config.setExposedHeaders(List.of(
                        "X-Total-Count","X-Total-Pages","X-Request-Id"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}