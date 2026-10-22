package com.managio.modules.portal.web;

import com.managio.platform.security.Principals.CustomerPrincipal;
import com.managio.platform.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Turning portal access off or archiving a customer takes effect on their next request, not when their
 * 15-minute access token expires.
 */
@Component
@RequiredArgsConstructor
class PortalActiveInterceptor implements HandlerInterceptor {

    private final JdbcClient jdbc;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomerPrincipal who) {
            boolean allowed = jdbc.sql("""
                            SELECT EXISTS (SELECT 1 FROM customers
                                           WHERE id = :id AND business_id = :businessId AND portal_enabled AND status = 'ACTIVE')""")
                    .param("id", who.id()).param("businessId", who.businessId()).query(Boolean.class).single();
            if (!allowed) {
                throw ApiException.unauthorized("auth.session_expired", "Your access has ended. Contact the business.");
            }
        }
        return true;
    }

    @Configuration
    @RequiredArgsConstructor
    static class Registration implements WebMvcConfigurer {

        private final PortalActiveInterceptor interceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(interceptor).addPathPatterns("/api/v1/portal/**").excludePathPatterns("/api/v1/portal/auth/**");
        }
    }
}
