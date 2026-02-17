package com.nitin.saas.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .exceptionHandling(exception -> exception
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/verify-email",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",
                                        "/api/health",
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/webjars/**",
                                        "/api-docs/**"
                                ).permitAll()

                                .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()

                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/v1/super-admin/**").hasRole("SUPER_ADMIN")

                                .anyRequest().authenticated()
                        )
                        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }
}